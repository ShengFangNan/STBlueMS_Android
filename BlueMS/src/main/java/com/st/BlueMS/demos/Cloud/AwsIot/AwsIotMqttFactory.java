/*
 * Copyright (c) 2017  STMicroelectronics – All rights reserved
 * The STMicroelectronics corporate logo is a trademark of STMicroelectronics
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions
 *   and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright notice, this list of
 *   conditions and the following disclaimer in the documentation and/or other materials provided
 *   with the distribution.
 *
 * - Neither the name nor trademarks of STMicroelectronics International N.V. nor any other
 *   STMicroelectronics company nor the names of its contributors may be used to endorse or
 *   promote products derived from this software without specific prior written permission.
 *
 * - All of the icons, pictures, logos and other images that are provided with the source code
 *   in a directory whose title begins with st_images may only be used for internal purposes and
 *   shall not be redistributed to any third party or modified in any way.
 *
 * - Any redistributions in binary form shall not include the capability to display any of the
 *   icons, pictures, logos and other images that are provided with the source code in a directory
 *   whose title begins with st_images.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY
 * OF SUCH DAMAGE.
 */

package com.st.BlueMS.demos.Cloud.AwsIot;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.Nullable;

import com.amazonaws.mobileconnectors.iot.AWSIotKeystoreHelper;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;
import com.st.BlueMS.demos.Cloud.CloutIotClientConnectionFactory;
import com.st.BlueMS.demos.Cloud.util.MqttClientUtil;
import com.st.BlueSTSDK.Feature;
import com.st.BlueSTSDK.Features.Field;
import com.st.BlueSTSDK.Node;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;


class AwsIotMqttFactory implements CloutIotClientConnectionFactory {

    private Uri mPrivateKey;

    private static class AwsConnection implements CloutIotClient{

        public final AWSIotMqttManager connection;
        public boolean isConnected=false;

        AwsConnection(AWSIotMqttManager manager){
            connection = manager;
        }
    }

    private static @Nullable AWSIotMqttManager extractConnection(CloutIotClient client){
        if(client==null || !(client instanceof AwsConnection))
            return null;
        return ((AwsConnection) client).connection;
    }

    private String mClientId;
    private String mEndpoint;
    private Uri mDeviceCertificate;

    AwsIotMqttFactory(String clientId,String endpoint, Uri deviceCertificate,Uri privateKey){
        mPrivateKey = privateKey;

        if(clientId==null || clientId.isEmpty())
            throw new IllegalArgumentException("Client Id Must Not be empty");
        if(endpoint==null || endpoint.isEmpty())
            throw new IllegalArgumentException("Invalid endpoint");

        mClientId = clientId;
        mEndpoint = endpoint;
        mDeviceCertificate = deviceCertificate;
    }


    @Override
    public CloutIotClient createClient(Context ctx) {

        AWSIotMqttManager manager = new AWSIotMqttManager(mClientId,mEndpoint);
        manager.setAutoReconnect(false);
        return new AwsConnection(manager);
    }

    @Override
    public boolean connect(Context ctx, CloutIotClient client, final ConnectionListener connectionListener) throws Exception {

        if(client instanceof AwsConnection) {
            new LoadKeyStoreAndConnect(ctx, (AwsConnection)client,
                    mDeviceCertificate, mPrivateKey, connectionListener).execute();
            return true;
        }

        return false;
    }

    @Override
    public void disconnect(CloutIotClient client) throws Exception {
        if(client instanceof AwsConnection) {
            AwsConnection connection = (AwsConnection)client;
            connection.connection.disconnect();
            connection.isConnected=false;
        }
    }


    private static class LoadKeyStoreAndConnect extends AsyncTask<Void,Void,KeyStore>{

        // Filename of KeyStore file on the filesystem
        private static final String KEYSTORE_NAME = "iot_keystore";
        // Password for the private key in the KeyStore
        private static final String KEYSTORE_PASSWORD = "password";
        // Certificate and key aliases in the KeyStore
        private static final String CERTIFICATE_ID = "default";

        private Context mContext;
        private final AwsConnection mConnection;
        private final Uri mCertificateFile;
        private final Uri mPrivateKey;
        private final ConnectionListener mListener;

        private String getKeystoreLocation(){
            return mContext.getFilesDir().getAbsolutePath();
        }

        LoadKeyStoreAndConnect(Context c,AwsConnection connection,
                                      Uri certificateFile, Uri privateKey, ConnectionListener listener){
            mContext = c;

            mConnection = connection;
            this.mCertificateFile = certificateFile;
            this.mPrivateKey = privateKey;
            this.mListener = listener;
        }

        private String loadFile(Uri uri) throws IOException {
            InputStream in = mContext.getContentResolver().openInputStream(uri);
            if (in == null)
                return  null;
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }
            return result.toString(StandardCharsets.UTF_8.name());
        }

        private void removeOldKeyStore(String path){
            if(AWSIotKeystoreHelper.isKeystorePresent(path,KEYSTORE_NAME))
                AWSIotKeystoreHelper.deleteKeystoreAlias(CERTIFICATE_ID,path,KEYSTORE_NAME,KEYSTORE_PASSWORD);
        }

        @Override
        protected KeyStore doInBackground(Void... params) {
            try {
                String certContent = loadFile(mCertificateFile);
                String prvKeyContent = loadFile(mPrivateKey);
                String keystorePath = getKeystoreLocation();
                removeOldKeyStore(keystorePath);
                AWSIotKeystoreHelper.saveCertificateAndPrivateKey(CERTIFICATE_ID,
                        certContent,
                        prvKeyContent,
                        keystorePath, KEYSTORE_NAME, KEYSTORE_PASSWORD);

                // load keystore from file into memory to pass on
                // connection
                return AWSIotKeystoreHelper.getIotKeystore(CERTIFICATE_ID,
                        keystorePath, KEYSTORE_NAME,KEYSTORE_PASSWORD);

            } catch (IOException | IllegalArgumentException e) {
                mListener.onFailure(e);
                e.printStackTrace();
            //if the certificate file has invalid format an array of of band is throw
            } catch (ArrayIndexOutOfBoundsException e){
                IllegalArgumentException newExc = new IllegalArgumentException("Invalid certificate format");
                mListener.onFailure(newExc);
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(KeyStore keyStore) {
            super.onPostExecute(keyStore);
            if(keyStore==null)
                return;
            mConnection.connection.connect(keyStore, (status, throwable) -> {
                if(throwable !=null){
                    mListener.onFailure(throwable);
                    return;
                }

                if(status == AWSIotMqttClientStatusCallback.AWSIotMqttClientStatus.Connected) {
                    mConnection.isConnected=true;
                    mListener.onSuccess();
                }
            });
        }
    }

    @Override
    public void destroy(CloutIotClient client) {  }

    @Override
    public boolean isConnected(CloutIotClient client) {
        if(client instanceof AwsConnection)
           return ((AwsConnection)client).isConnected;
        return false;
    }

    @Override
    public Feature.FeatureListener getFeatureListener(CloutIotClient client) {
        if(client instanceof AwsConnection)
            return new AwsMqttFeatureListener(mClientId,(AwsConnection)client);
        return null;
    }

    @Nullable
    @Override
    public Uri getDataPage() {
        return null;
    }

    @Override
    public boolean supportFeature(Feature f) {
        return MqttClientUtil.isSupportedFeature(f);
    }

    @Override
    public boolean enableCloudFwUpgrade(Node node, CloutIotClient cloudConnection, FwUpgradeAvailableCallback callback) {
        return false;
    }

    /**
     * feature listener that will send all the update to the mqtt broker.
     * each update is published as a string in the topic:
     * ClientId/FeatureName/FieldName
     */
    private static class AwsMqttFeatureListener implements Feature.FeatureListener {

        private AwsConnection mConnection;
        private String mClientId;

        /**
         * build an object that will publish all the update to the cloud
         * @param clientId name of the device that generate the data
         * @param client object where publish the data
         */
        AwsMqttFeatureListener(String clientId,AwsConnection client) {
            mConnection = client;
            mClientId=clientId;
        }

        @Override
        public void onUpdate(Feature f, Feature.Sample sample) {
            if(!mConnection.isConnected)
                return;
            Field fields[] = sample.dataDesc;
            Number data[] = sample.data;
            for(int i =0; i<data.length ; i++){
                String topic = MqttClientUtil.getPublishTopic(mClientId,f.getName(),
                        fields[i].getName());
                mConnection.connection.publishString(data[i].toString(),
                        topic,
                        AWSIotMqttQos.QOS0);
            }
        }//onUpdate
    }//

}
