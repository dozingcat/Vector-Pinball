package com.dozingcatsoftware.vectorpinball.editor.tools;

import static com.dozingcatsoftware.vectorpinball.util.MathUtils.asDouble;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;

import com.badlogic.gdx.math.Vector2;
import com.dozingcatsoftware.vectorpinball.groovy.GroovyFieldDelegateBuilder;
import com.dozingcatsoftware.vectorpinball.model.AudioPlayer;
import com.dozingcatsoftware.vectorpinball.model.Ball;
import com.dozingcatsoftware.vectorpinball.model.Field;
import com.dozingcatsoftware.vectorpinball.model.IStringResolver;
import com.dozingcatsoftware.vectorpinball.util.JSONUtils;

/**
 * Command line tool that loads a table JSON file and simulates a game without any UI:
 * launches balls, randomly flips the flippers, and runs the physics and script delegate
 * for a number of simulated seconds. Fails with an exception if the table can't be
 * loaded or anything goes wrong during simulation. Usage:
 *
 *   TableSmokeTest table.json [simulatedSeconds]
 */
public class TableSmokeTest {

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: TableSmokeTest table.json [simulatedSeconds]");
            System.exit(1);
        }
        String json = new String(Files.readAllBytes(Paths.get(args[0])), StandardCharsets.UTF_8);
        Map<String, Object> fieldMap = JSONUtils.mapFromJSONString(json);
        long simSeconds = (args.length > 1) ? Long.parseLong(args[1]) : 120;

        IStringResolver stringResolver = (key, params) -> key;
        Function<Field, Field.Delegate> delegateFn = (f -> {
            String script = f.getScriptText();
            if (script != null && script.trim().length() > 0) {
                return GroovyFieldDelegateBuilder.createFromScript(
                        script, TableSmokeTest.class.getClassLoader());
            }
            return Field.createDelegateFromLayoutClass(f);
        });

        Field field = new Field(System::currentTimeMillis, stringResolver,
                AudioPlayer.NoOpPlayer.getInstance());
        field.resetForLayoutMap(fieldMap, delegateFn);
        field.startGame();

        double fieldWidth = asDouble(fieldMap.get("width"), 20);
        double fieldHeight = asDouble(fieldMap.get("height"), 30);

        Random random = new Random(0);
        long tickNanos = 16_666_667L * 2;  // ~60fps frames at 2x time ratio, 4 physics iterations
        long totalNanos = simSeconds * 1_000_000_000L;
        long elapsed = 0;
        int launches = 0;
        while (elapsed < totalNanos && field.getGameState().isGameInProgress()) {
            if (field.getBalls().isEmpty()) {
                field.launchBallIfNeeded();
                launches++;
            }
            // Flip randomly: hold each side for a random interval.
            if (random.nextInt(20) == 0) {
                field.setLeftFlippersEngaged(random.nextBoolean());
                field.setRightFlippersEngaged(random.nextBoolean());
            }
            field.tick(tickNanos, 4);
            elapsed += tickNanos;
            // A ball outside the field means it escaped through a gap in the walls; balls that
            // drain normally are removed by kill walls before they get this far out.
            for (Ball ball : field.getBalls()) {
                Vector2 pos = ball.getPosition();
                if (pos.x < -0.5 || pos.x > fieldWidth + 0.5 || pos.y < -1 || pos.y > fieldHeight + 1) {
                    System.out.println("FAILED: ball escaped the field at ("
                            + pos.x + ", " + pos.y + ") after "
                            + (elapsed / 1_000_000_000L) + "s");
                    System.exit(1);
                }
            }
        }

        System.out.println("Simulated " + (elapsed / 1_000_000_000L) + "s of game time");
        System.out.println("Balls launched: " + launches);
        System.out.println("Game in progress at end: " + field.getGameState().isGameInProgress());
        System.out.println("Final score: " + field.getGameState().getScore());
        System.out.println("OK");
    }
}
