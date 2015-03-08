package com.dozingcatsoftware.bouncy;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.widget.TextView;

public class AboutActivity extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.about);

        // Get text to display by replacing "[TABLE_RULES]" with the contents of string resource
        // with ID table[level]_rules.
        String baseText = getString(R.string.about_text);
        String tableRulesText = null;
        try {
            String fieldName = "table" + getIntent().getIntExtra("level", 1) + "_rules";
            int tableRulesID = (Integer)R.string.class.getField(fieldName).get(null);
            tableRulesText = getString(tableRulesID);
        }
        catch(Exception ex) {
            tableRulesText = null;
        }
        if (tableRulesText==null) tableRulesText = "";
        String displayText = baseText.replace("[TABLE_RULES]", tableRulesText);

        TextView tv = (TextView)findViewById(R.id.aboutTextView);
        tv.setText(displayText);
    }

    public static Intent startForLevel(Context context, int level) {
        Intent aboutIntent = new Intent(context, AboutActivity.class);
        aboutIntent.putExtra("level", level);
        context.startActivity(aboutIntent);
        return aboutIntent;
    }

}
