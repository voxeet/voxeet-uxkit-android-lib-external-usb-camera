package com.voxeet.sdk.external_usb.sample;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.voxeet.VoxeetSDK;
import com.voxeet.android.media.MediaStream;
import com.voxeet.android.media.stream.MediaStreamType;
import com.voxeet.promise.solve.ErrorPromise;
import com.voxeet.promise.solve.ThenPromise;
import com.voxeet.sdk.events.v2.ParticipantAddedEvent;
import com.voxeet.sdk.events.v2.ParticipantUpdatedEvent;
import com.voxeet.sdk.events.v2.StreamAddedEvent;
import com.voxeet.sdk.events.v2.StreamRemovedEvent;
import com.voxeet.sdk.events.v2.StreamUpdatedEvent;
import com.voxeet.sdk.external_usb.camera.ExternalCameraCapturerProvider;
import com.voxeet.sdk.json.ParticipantInfo;
import com.voxeet.sdk.models.Conference;
import com.voxeet.sdk.models.Participant;
import com.voxeet.sdk.services.conference.information.ConferenceInformation;
import com.voxeet.sdk.utils.Map;
import com.voxeet.sdk.utils.Opt;
import com.voxeet.sdk.views.VideoView;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class USBVoxeetActivity extends AppCompatActivity {
    @NonNull
    protected List<View> views = new ArrayList<>();

    @NonNull
    protected List<View> buttonsNotLoggedIn = new ArrayList<>();

    @NonNull
    protected List<View> buttonsInConference = new ArrayList<>();

    @NonNull
    protected List<View> buttonsNotInConference = new ArrayList<>();

    @NonNull
    @Bind(R.id.conference_name)
    EditText conference_name;

    @Bind(R.id.user_name)
    EditText user_name;

    @Bind(R.id.video)
    protected VideoView video;

    @Bind(R.id.videoOther)
    protected VideoView videoOther;

    @Bind(R.id.participants)
    EditText participants;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        //throw new IllegalStateException("<---- Remove this line and set your keys below to use this sample !!");
        VoxeetSDK.initialize("", "");

        //adding the user_name, login and logout views related to the open/close and conference flow
        add(views, R.id.login);
        add(views, R.id.logout);
        add(views, R.id.user_name);
        add(views, R.id.join);
        add(views, R.id.leave);
        add(views, R.id.startVideo);
        add(views, R.id.stopVideo);

        add(buttonsNotLoggedIn, R.id.login);
        add(buttonsNotLoggedIn, R.id.user_name);

        add(buttonsInConference, R.id.logout);
        add(buttonsInConference, R.id.leave);
        add(buttonsInConference, R.id.startVideo);
        add(buttonsInConference, R.id.stopVideo);

        add(buttonsNotInConference, R.id.logout);

        add(views, R.id.join);

        add(buttonsNotInConference, R.id.join);

        add(views, R.id.leave);

        add(buttonsInConference, R.id.leave);
    }

    @Override
    protected void onResume() {
        super.onResume();

        updateViews();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA}, 0x20);
        }

        VoxeetSDK.instance().register(this);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    private USBVoxeetActivity add(List<View> list, int id) {
        list.add(findViewById(id));
        return this;
    }

    private void updateViews() {
        //this method will be updated step by step
        //disable every views
        setEnabled(views, false);

        //if the user is not connected, we will only enabled the not logged
        if (!VoxeetSDK.session().isSocketOpen()) {
            setEnabled(buttonsNotLoggedIn, true);
            return;
        }

        ConferenceInformation current = VoxeetSDK.conference().getCurrentConference();
        //we can now add the logic to manage our basic state
        if (null != current && VoxeetSDK.conference().isLive()) {
            setEnabled(buttonsInConference, true);
        } else {
            setEnabled(buttonsNotInConference, true);
        }
    }

    private ErrorPromise error() {
        return error -> {
            Toast.makeText(USBVoxeetActivity.this, "ERROR...", Toast.LENGTH_SHORT).show();
            error.printStackTrace();
            updateViews();
        };
    }

    private void setEnabled(@NonNull List<View> views, boolean enabled) {
        for (View view : views) view.setEnabled(enabled);
    }

    @OnClick(R.id.login)
    public void onLogin() {
        VoxeetSDK.session().open(new ParticipantInfo(user_name.getText().toString(), "", ""))
                .then((result, solver) -> {
                    Toast.makeText(USBVoxeetActivity.this, "started...", Toast.LENGTH_SHORT).show();
                    updateViews();
                })
                .error(error());
    }

    @OnClick(R.id.logout)
    public void onLogout() {
        VoxeetSDK.session().close()
                .then((result, solver) -> {
                    Toast.makeText(USBVoxeetActivity.this, "logout done", Toast.LENGTH_SHORT).show();
                    updateViews();
                }).error(error());
    }

    @OnClick(R.id.join)
    public void onJoin() {
        VoxeetSDK.conference().create(conference_name.getText().toString())
                .then((ThenPromise<Conference, Conference>) res -> VoxeetSDK.conference().join(res.getId()))
                .then(conference -> {
                    Toast.makeText(USBVoxeetActivity.this, "started...", Toast.LENGTH_SHORT).show();
                    updateViews();
                })
                .error(error());
    }

    @OnClick(R.id.leave)
    public void onLeave() {
        VoxeetSDK.conference().leave()
                .then((result, solver) -> updateViews()).error(error());
    }

    @OnClick(R.id.startVideo)
    public void onStartVideo() {
        VoxeetSDK.screenShare().startCustomScreenShare(new ExternalCameraCapturerProvider(USBVoxeetActivity.this))
                .then((result, solver) -> updateViews())
                .error(error());
    }

    @OnClick(R.id.stopVideo)
    public void onStopVideo() {
        VoxeetSDK.screenShare().stopScreenShare()
                .then((result, solver) -> updateViews())
                .error(error());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(StreamAddedEvent event) {
        updateStreams();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(StreamUpdatedEvent event) {
        updateStreams();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(StreamRemovedEvent event) {
        updateStreams();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(ParticipantAddedEvent event) {
        updateUsers();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(ParticipantUpdatedEvent event) {
        updateUsers();
    }

    private void updateStreams() {
        for (Participant user : VoxeetSDK.conference().getParticipants()) {
            checkStreamForParticipant(user);
        }
    }

    private void checkStreamForParticipant(@Nullable Participant user) {
        boolean isLocal = user.getId().equals(VoxeetSDK.session().getParticipantId());
        MediaStream stream = user.streamsHandler().getFirst(MediaStreamType.ScreenShare);

        VideoView video = isLocal ? this.video : this.videoOther;

        if (null != stream && !stream.videoTracks().isEmpty()) {
            video.setVisibility(View.VISIBLE);
            video.attach(user.getId(), stream);
            video.setBackgroundColor(getResources().getColor(R.color.colorBlack));
        }
    }

    public void updateUsers() {
        List<String> names = Map.map(VoxeetSDK.conference().getParticipants(), participant ->
                Opt.of(participant.getInfo()).then(ParticipantInfo::getName).or(""));

        this.participants.setText(TextUtils.join(",", names));
    }
}