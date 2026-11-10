package com.dozingcatsoftware.vectorpinball.editor;

import com.dozingcatsoftware.vectorpinball.model.IStringResolver;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

public class StringResolver implements IStringResolver {

    static Map<String, MessageFormat> MESSAGES = createMessageMap();

    static MessageFormat mf(String s) {
        return new MessageFormat(s);
    }

    static Map<String, MessageFormat> createMessageMap() {
        Map<String, MessageFormat> m = new HashMap<>();
        m.put("ball_number_message", mf("Ball {0}"));
        m.put("shoot_again_message", mf("Shoot Again"));
        m.put("game_over_message", mf("Game Over"));
        m.put("bump_message", mf("Bump!"));
        m.put("touch_to_start_message", mf("Touch to start"));
        m.put("last_score_message", mf("Last Score: {0}"));
        m.put("top_high_score_message", mf("High Score: {0}"));
        m.put("other_high_score_message", mf("#{0} Score: {1}"));
        m.put("left_save_enabled_message", mf("Left Save Enabled"));
        m.put("right_save_enabled_message", mf("Right Save Enabled"));
        m.put("shoot_ramp_multiball_message", mf("Shoot Ramp for Multiball"));
        m.put("multiball_started_message", mf("Multiball!"));
        m.put("multiplier_message", mf("{0}x Multiplier"));
        m.put("shoot_red_bumper_message", mf("Shoot Red Bumper"));
        m.put("bumper_multiplier_message", mf("Bumper Multiplier {0}"));
        m.put("jackpot_received_message", mf("Jackpot!"));
        m.put("jackpot_received_with_multipler_message", mf("{0} Jackpot!"));
        m.put("multiball_ready_message", mf("Multiball Ready"));
        m.put("ball_lock_ready_message", mf("Ball Lock Ready"));
        m.put("ball_locked_message", mf("Ball {0} Locked"));
        m.put("shoot_pyramid_message", mf("Shoot the Pyramid!"));
        m.put("extra_ball_received_message", mf("Extra Ball!"));
        m.put("blue_ramp_bonus_message", mf("Blue ramp +{0}%"));
        m.put("red_ramp_bonus_message", mf("Red ramp +{0}%"));
        m.put("yellow_ramp_bonus_message", mf("Yellow ramp +{0}%"));
        m.put("green_ramp_bonus_message", mf("Green ramp +{0}%"));
        m.put("color_blue", mf("Blue"));
        m.put("color_red", mf("Red"));
        m.put("color_yellow", mf("Yellow"));
        m.put("color_green", mf("Green"));
        m.put("planet_activated_message", mf("Planet {0} Activated!"));
        m.put("ramp_bonus_message", mf("{0} Ramp"));
        m.put("ramp_bonus_increased_message", mf("Ramp bonus increased"));
        m.put("shoot_ramp_jackpot_message", mf("Shoot ramp for jackpot"));
        m.put("constellation_complete_message", mf("{0} complete"));
        m.put("ball_saved_message", mf("Ball saved"));
        return m;
    }

    @Override public String resolveString(String key, Object... params) {
        MessageFormat mf = MESSAGES.get(key);
        if (mf == null) {
            return "UNKNOWN KEY: " + key;
        }
        return mf.format(params);
    }
}
