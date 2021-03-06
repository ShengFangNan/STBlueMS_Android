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

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.st.BlueMS.R;
import com.st.BlueMS.demos.Cloud.CloutIotClientConfigurationFactory;
import com.st.BlueMS.demos.Cloud.CloutIotClientConnectionFactory;
import com.st.BlueMS.demos.Cloud.util.InputChecker.CheckRegularExpression;
import com.st.BlueMS.demos.Cloud.util.MqttClientUtil;
import com.st.BlueSTSDK.Node;

import java.util.regex.Pattern;


public class AwSIotConfigurationFactory implements CloutIotClientConfigurationFactory {

    private static final String NAME = "AWS IoT";
    private static final String CONFIG_FRAGMENT_TAG = AwsConfigFragment.class.getCanonicalName();

    private AwsConfigFragment mConfigFragment;

    @Override
    public void attachParameterConfiguration(Context c, ViewGroup root) {
        Activity a = (Activity)c;

        //check if a fragment is already attach, and remove it to attach the new one
        mConfigFragment = (AwsConfigFragment)
                a.getFragmentManager().findFragmentByTag(CONFIG_FRAGMENT_TAG);
        AwsConfigFragment newFragment = new AwsConfigFragment();

        FragmentTransaction transaction = a.getFragmentManager().beginTransaction();
        if(mConfigFragment==null)
            transaction.add(root.getId(),newFragment,CONFIG_FRAGMENT_TAG);
        else
            transaction.replace(root.getId(),newFragment,CONFIG_FRAGMENT_TAG);

        transaction.commit();
        mConfigFragment = newFragment;

    }

    @Override
    public void loadDefaultParameters(@Nullable Node n) {
        mConfigFragment.setClientId(MqttClientUtil.getDefaultCloudDeviceName(n));
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public CloutIotClientConnectionFactory getConnectionFactory() throws IllegalArgumentException {
        return new AwsIotMqttFactory(mConfigFragment.getClientId(),mConfigFragment.getEndpoint(),
               mConfigFragment.getCertificateFile(),mConfigFragment.getPrivateKeyFile());
    }



}
