package wayfarer.mhacks.com.wayfarer;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements ActionBar.TabListener{

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide fragments for each of the
     * three primary sections of the app. We use a {@link FragmentPagerAdapter}
     * derivative, which will keep every loaded fragment in memory. If this becomes too memory
     * intensive, it may be best to switch to a {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    AppSectionsPagerAdapter mAppSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will display the three primary sections of the app, one at a
     * time.
     */
    ViewPager mViewPager;

    String TWILIO_NUMBER  = "+17347864591";
    boolean walk, drive, call, text;
    HashMap<String, String> msg = new HashMap<String, String>(){{
        put("walk", "w");
        put("drive", "d");
        put("call", "c");
        put("text", "t");
        put("esri", "e");
        put("gmaps", "g");
    }};

    String transType = "d";
    String commType = "t";
    String mapType = "g";


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        // Hiding the title bar has to happen before the view is created

        setContentView(R.layout.activity_main);

        transType = msg.get("drive");
        commType = msg.get("text");
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, 1);

        // Create the adapter that will return a fragment for each of the three primary sections
        // of the app.
        mAppSectionsPagerAdapter = new AppSectionsPagerAdapter(getSupportFragmentManager());
        // Set up the action bar.
        final ActionBar actionBar = getSupportActionBar();

        // Specify that the Home/Up button should not be enabled, since there is no hierarchical
        // parent.
        actionBar.setHomeButtonEnabled(false);
        actionBar.setDisplayShowTitleEnabled(false);

        // Specify that we will be displaying tabs in the action bar.
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // Set up the ViewPager, attaching the adapter and setting up a listener for when the
        // user swipes between sections.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mAppSectionsPagerAdapter);
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                // When swiping between different app sections, select the corresponding tab.
                // We can also use ActionBar.Tab#select() to do this if we have a reference to the
                // Tab.
                actionBar.setSelectedNavigationItem(position);
            }
        });

        // For each of the sections in the app, add a tab to the action bar.
        for (int i = 0; i < mAppSectionsPagerAdapter.getCount(); i++) {
            // Create a tab with text corresponding to the page title defined by the adapter.
            // Also specify this Activity object, which implements the TabListener interface, as the
            // listener for when this tab is selected.
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(mAppSectionsPagerAdapter.getPageTitle(i))
                            .setTabListener(this));
        }
    }

    private BroadcastReceiver mIntentReceiver;
    private BroadcastReceiver mmsIntentReceiver;
    @Override
    protected void onResume() {
        super.onResume();

        //SMS
        IntentFilter intentFilter = new IntentFilter("SmsMessage.intent.MAIN");
        mIntentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String msg = intent.getStringExtra("get_msg");

//Process the sms format and extract body &amp; phoneNumber
                String body = msg.substring(msg.lastIndexOf(":")+1, msg.length());
                String pNumber = msg.substring(0, msg.lastIndexOf(":"));
                if(pNumber.equals(TWILIO_NUMBER)){
                    String ls = System.getProperty("line.separator");
                    //body = body.replaceAll(" __", ls);
                    Log.v("Final", body);

                    TextView t = (TextView)findViewById(R.id.directions);
                    Button dir = (Button)findViewById(R.id.dirlabel);
                    dir.setVisibility(View.VISIBLE);
                    t.setText(body);
                    }

            }
        };
        this.registerReceiver(mIntentReceiver, intentFilter);

        //MMS
        IntentFilter mmsIntentFilter = new IntentFilter("MmsMessage.intent.MAIN");
        mmsIntentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bitmap bitmap = null;
                String mmsMsg = intent.getStringExtra("mms");
                String mmsTrans = intent.getStringExtra("transactionId");
                String mmsId = intent.getStringExtra("mmsId");

                    Log.d("FinalMMS", mmsMsg);
                    Log.d("FinalMMSTrans", mmsTrans);
                    Log.d("FinalMMSId", mmsId);

                String selectionPart = "mid=" + mmsTrans;
                Uri uri = Uri.parse("content://mms/part");
                Cursor cPart = getContentResolver().query(uri, null,
                        selectionPart, null, null);
                if (cPart.moveToFirst()) {
                    do {
                        String partId = cPart.getString(cPart.getColumnIndex("_id"));
                        String type = cPart.getString(cPart.getColumnIndex("ct"));
                        if ("image/jpeg".equals(type) || "image/bmp".equals(type) ||
                                "image/gif".equals(type) || "image/jpg".equals(type) ||
                                "image/png".equals(type)) {
                             bitmap = getMmsImage(partId);
                        }
                    } while (cPart.moveToNext());
                }
                /* TODO: Fix image
                ImageView mapimg = (ImageView)findViewById(R.id.mapimg);
                mapimg.setImageBitmap(bitmap);
                */

            }
        };
        this.registerReceiver(mmsIntentReceiver, mmsIntentFilter);
    }

    private Bitmap getMmsImage(String _id) {
        Uri partURI = Uri.parse("content://mms/part/" + _id);
        InputStream is = null;
        Bitmap bitmap = null;
        try {
            is = getContentResolver().openInputStream(partURI);
            bitmap = BitmapFactory.decodeStream(is);
        } catch (IOException e) {}
        finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {}
            }
        }
        return bitmap;
    }

    public void transportation(View view) {
        boolean check = ((RadioButton) view).isChecked();

        switch(view.getId()) {
            case R.id.driving:
                if(check)
                    transType = msg.get("drive");
                break;
            case R.id.walking:
                if(check)
                    transType = msg.get("walk");
                break;
        }
    }

    public void communication(View view) {
        boolean check = ((RadioButton) view).isChecked();

        switch(view.getId()) {
            case R.id.calling:
                if(check)
                    commType = msg.get("call");
                break;
            case R.id.texting:
                if(check)
                    commType = msg.get("text");
                break;
        }
    }

    public void map_type(View view) {
        boolean check = ((RadioButton) view).isChecked();

        switch(view.getId()) {
            case R.id.esri:
                if(check)
                    commType = msg.get("esri");
                break;
            case R.id.gmaps:
                if(check)
                    commType = msg.get("gmaps");
                break;
        }
    }

    public void directions(View view) {
        EditText mysource = (EditText) findViewById(R.id.editText);
        EditText mydestination = (EditText) findViewById(R.id.editText2);
        String sSource = mysource.getText().toString();
        String sDestination = mydestination.getText().toString();

        if(sSource.isEmpty() || sDestination.isEmpty()){
            Toast toast = Toast.makeText(getApplicationContext(), "Please enter valid values for \"From\" and \"Destination\".", Toast.LENGTH_SHORT);
            toast.show();

        }
        else {
            String getdircmd = "dir" + transType + commType + "//" + sSource + "//" + sDestination;
            Toast toast = Toast.makeText(getApplicationContext(), "Fetching directions." + System.getProperty("line.separator") + "This may take up to 30 seconds.", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(TWILIO_NUMBER, null, getdircmd, null, null);
        }

    }

    public void map(View view) {
        EditText mylocation = (EditText) findViewById(R.id.editLocation);
        String sLocation = mylocation.getText().toString();

        if(sLocation.isEmpty() ){
            Toast toast = Toast.makeText(getApplicationContext(), "Please enter valid values for \"Location\".", Toast.LENGTH_SHORT);
            toast.show();
        }
        else {

            String getmapcmd = "map" + mapType + "//" + sLocation;
            Toast toast = Toast.makeText(getApplicationContext(), "Fetching map." + System.getProperty("line.separator") + "This may take up to 30 seconds.", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(TWILIO_NUMBER, null, getmapcmd, null, null);
        }

    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
        // When the given tab is selected, switch to the corresponding page in the ViewPager.
        mViewPager.setCurrentItem(tab.getPosition());
    }
    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {

    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {

    }


    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to one of the primary
     * sections of the app.
     */
    public static class AppSectionsPagerAdapter extends FragmentPagerAdapter {

        public AppSectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            switch (i) {
                case 0:
                    // The first section of the app is the most interesting -- it offers
                    // a launchpad into the other demonstrations in this example application.
                    return new LaunchpadSectionFragment();

                default:
                    // The other sections of the app are dummy placeholders.
                    Fragment fragment = new DummySectionFragment();
                    Bundle args = new Bundle();
                    args.putInt(DummySectionFragment.ARG_SECTION_NUMBER, i + 1);
                    fragment.setArguments(args);
                    return fragment;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if(position==0){
                return "Directions";
            }else{
                return "Map";
            }
        }
    }

    /**
     * A fragment that launches other parts of the demo application.
     */
    public static class LaunchpadSectionFragment extends Fragment {

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_directions, container, false);


            return rootView;
        }
    }

    /**
     * A dummy fragment representing a section of the app, but that simply displays dummy text.
     */
    public static class DummySectionFragment extends Fragment {

        public static final String ARG_SECTION_NUMBER = "section_number";

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_map, container, false);
            Bundle args = getArguments();
//            ((TextView) rootView.findViewById(android.R.id.text1)).setText(
//                    getString(R.string.dummy_section_text, args.getInt(ARG_SECTION_NUMBER)));
            return rootView;
        }
    }
}


