package org.libertaria.world.utils;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import org.libertaria.world.core.pure.KeyEd25519Java;
import org.libertaria.world.profile_server.model.KeyEd25519;
import org.libertaria.world.profiles_manager.ProfileOuterClass;
import org.junit.Test;

/**
 * Created by furszy on 6/30/17.
 */

public class BackupTest {

    @Test
    public void testBackup() throws InvalidProtocolBufferException {

        KeyEd25519 keyEd25519 = new KeyEd25519Java().generateKeys();

        ProfileOuterClass.ProfileInfo profileInfo = ProfileOuterClass.ProfileInfo.newBuilder()
                .setVersion(ByteString.copyFrom(new byte[]{0,0,1}))
                .setName("carlos")
                .setType("chat")
                .setExtraData("extra_data")
                .setHomeHost("192.168.0.1")
                .setPubKey(ByteString.copyFrom(keyEd25519.getPublicKey()))
                .build();



        ProfileOuterClass.Profile profile = ProfileOuterClass.Profile.
                newBuilder()
                .setProfileInfo(profileInfo)
                .setPrivKey(ByteString.copyFrom(keyEd25519.getPrivateKey()))
                .build();

        ProfileOuterClass.Wrapper backup = ProfileOuterClass.Wrapper.newBuilder().setProfile(profile).addProfilesInfo(profileInfo).build();

        byte[] fileBytes = backup.toByteArray();


        // now restore and check if it's the same

        ProfileOuterClass.Wrapper backupRestored = ProfileOuterClass.Wrapper.parseFrom(fileBytes);

        assert backup.getProfile().getPrivKey().equals(backupRestored.getProfile().getPrivKey());

    }


}
