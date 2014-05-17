package me.ycdev.android.trafficanalyzer;

import java.io.IOException;

import me.ycdev.android.trafficanalyzer.R;
import me.ycdev.android.trafficanalyzer.profile.AppProfile;
import me.ycdev.android.trafficanalyzer.stats.StatsParseException;
import me.ycdev.android.trafficanalyzer.stats.StatsSnapshot;
import me.ycdev.android.trafficanalyzer.stats.UidTrafficStats;
import me.ycdev.android.trafficanalyzer.utils.AppLogger;
import me.ycdev.androidlib.base.WeakHandler;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

public class AppTrafficUsageActivity extends Activity implements WeakHandler.MessageHandler {
    private static final boolean DEBUG = AppLogger.DEBUG;
    private static final String TAG = "AppTrafficUsageActivity";

    private static final String EXTRA_UID = "extra.uid";
    private static final String EXTRA_OLD_SNAPSHOT = "extra.oldsnap";
    private static final String EXTRA_NEW_SNAPSHOT = "extra.newsnap";

    private int mAppUid;
    private StatsSnapshot mOldSnapshot;
    private StatsSnapshot mNewSnapshot;

    private UidTrafficStats mOldUidStats;
    private UidTrafficStats mNewUidStats;
    private UidTrafficStats mUidUsage;

    private CheckBox mFgTrafficCheckBox;
    private CheckBox mBgTrafficCheckBox;
    private GridView mIfaceChoicesView;
    private TableLayout mTagsStatsView;

    private Handler mHandler = new WeakHandler(this);

    @Override
    public void handleMessage(Message msg) {
        // nothing to do
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        mAppUid = intent.getIntExtra(EXTRA_UID, -1);
        mOldSnapshot = intent.getParcelableExtra(EXTRA_OLD_SNAPSHOT);
        mNewSnapshot = intent.getParcelableExtra(EXTRA_NEW_SNAPSHOT);
        if (mAppUid == -1 || mOldSnapshot == null || mNewSnapshot == null) {
            AppLogger.w(TAG, "bad arguments, uid: " + mAppUid + ", snap1: " + mOldSnapshot
                    + ", snap2: " + mNewSnapshot);
            finish();
            return;
        }

        initViews();
        loadData();
    }

    private void initViews() {
        setContentView(R.layout.app_traffic_usage);

        TextView snap1View = (TextView) findViewById(R.id.snapshot_old);
        snap1View.setText(getString(R.string.usage_snapshot_old, mOldSnapshot.fileName));
        TextView snap2View = (TextView) findViewById(R.id.snapshot_new);
        snap2View.setText(getString(R.string.usage_snapshot_new, mNewSnapshot.fileName));

        mFgTrafficCheckBox = (CheckBox) findViewById(R.id.fg_traffic);
        mFgTrafficCheckBox.setChecked(true);
        mBgTrafficCheckBox = (CheckBox) findViewById(R.id.bg_traffic);
        mBgTrafficCheckBox.setChecked(true);

        mIfaceChoicesView = (GridView) findViewById(R.id.iface_choices);

        mTagsStatsView = (TableLayout) findViewById(R.id.tags_stats);
    }

    private void loadData() {
        final ProgressDialog dlg = new ProgressDialog(this);
        dlg.setMessage(getString(R.string.tips_load_data));
        dlg.setCancelable(false);
        dlg.show();

        final Context context = getApplicationContext();
        new Thread() {
            @Override
            public void run() {
                boolean loadSuccess = false;
                try {
                    if (DEBUG) AppLogger.i(TAG, "parsing old snapshot..." + mOldSnapshot.fileName);
                    mOldUidStats = mOldSnapshot.parse(mAppUid);
                    if (DEBUG) AppLogger.i(TAG, "parsing new snapshot..." + mNewSnapshot.fileName);
                    mNewUidStats = mNewSnapshot.parse(mAppUid);
                    loadSuccess = true;
                } catch (IOException e) {
                    AppLogger.w(TAG, "failed to load uid stats", e);
                } catch (StatsParseException e) {
                    AppLogger.w(TAG, "failed to load uid stats", e);
                }

                if (loadSuccess) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            dlg.setMessage(context.getString(R.string.tips_computing_usage_ongoing));
                        }
                    });
                    if (DEBUG) AppLogger.i(TAG, "computing usage...");
                    mUidUsage = mNewUidStats.subtract(mOldUidStats);
                }

                if (!loadSuccess) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, R.string.tips_computing_usage_failed,
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                }
                dlg.dismiss();
            }
        }.start();
    }

    private void computeTrafficUsage() {
        
    }

    public static void showTrafficUsage(Context cxt, AppProfile appProfile,
            StatsSnapshot oldSnapshot, StatsSnapshot newSnapshot) {
        Intent intent = new Intent(cxt, AppTrafficUsageActivity.class);
        intent.putExtra(EXTRA_UID, appProfile.getAppUid());
        intent.putExtra(EXTRA_OLD_SNAPSHOT, oldSnapshot);
        intent.putExtra(EXTRA_NEW_SNAPSHOT, newSnapshot);
        cxt.startActivity(intent);
    }

}
