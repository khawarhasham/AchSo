/**
 * Copyright 2013 Aalto university, see AUTHORS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fi.aalto.legroup.achso.state;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import java.util.concurrent.ExecutionException;

import fi.aalto.legroup.achso.activity.ActionbarActivity;
import fi.aalto.legroup.achso.util.App;
import fi.aalto.legroup.achso.util.LasConnection;

import static fi.aalto.legroup.achso.util.App.appendLog;

public class LasLoginState {
    public static final int LOGGED_OUT = 0;
    private int mIn = LOGGED_OUT;
    public static final int TRYING_TO_LOG_IN = 1;
    public static final int LOGGED_IN = 2;
    private String mUser = null;
    private Context ctx;
    private Activity mHost;
    private boolean disable_autologin_for_session = false;

    public LasLoginState(Context ctx) {
        this.ctx = ctx;
    }

    public int getLoginStatus() {
        return mIn;
    }

    public void asynchronousLogin(String user, String pass) {
        setState(LOGGED_OUT);
        mUser = user;
        if (user.isEmpty() || pass.isEmpty()) {
            return;
        } else {
            AsyncTask<String, Void, String> task = new LoginTask();
            task.execute(user, pass);
            setState(TRYING_TO_LOG_IN);
        }

    }

    public void setHostActivity(Activity host) {
        mHost = host;
    }

    public void autologinIfAllowed() {
        if (!disable_autologin_for_session) {
            if (mIn == LOGGED_OUT && App.hasConnection()) {
                SharedPreferences prefs = ctx.getSharedPreferences("AchSoPrefs", 0);
                boolean allowed = prefs.getBoolean("autologin", false);
                String login = prefs.getString("login", "");
                String pwd = prefs.getString("pwd", "");
                appendLog(String.format("Doing autologin as %s", mUser));

                if (allowed && !login.isEmpty() && !pwd.isEmpty()) {
                    asynchronousLogin(login, pwd);
                    Log.i("LoginState", "autologin launched async login.");
                }

            }
        }
    }

    public boolean login(String user, String pass) {
        setState(LOGGED_OUT);
        appendLog(String.format("Doing manual login as %s", user));

        if (user == null || pass == null) {
            return false;
        } else {
            // TODO (Petru) Replace the old LAS login with the integrated OpenID login

            //HTTP call - after Android > 3.x the AsyncTask should be used to avoid the android.os.NetworkOnMainThreadException
            //The login uses the util.LasConnection class (taken from AnViAnno) and the http-connector-client.jar library
            AsyncTask<String, Void, String> taskresult = new LoginTask().execute(user, pass);
            setState(TRYING_TO_LOG_IN);

            // The result of the login operation will be a session id
            String result;
            try {
                result = taskresult.get();
            } catch (InterruptedException e) {
                Log.e("LoginState", "Login failed, catched InterruptedException");
                e.printStackTrace();
                setState(LOGGED_OUT);
                return false;
            } catch (ExecutionException e) {
                Log.e("LoginState", "Login failed, catched ExecutionException");
                e.printStackTrace();
                setState(LOGGED_OUT);
                return false;
            }
            // LoginTask onPostExecute sets mIn to LOGGED_IN or LOGGED_OUT
            if (result.equals(LasConnection.CONNECTION_PROBLEM)) {
                Log.e("LoginState", "iLogin failed, connection problem");
                setState(LOGGED_OUT);
            } else if (result.equals(LasConnection.AUTHENTICATION_PROBLEM)) {
                Log.e("LoginState", "iLogin failed, authentication problem");
                setState(LOGGED_OUT);
            } else if (result.equals(LasConnection.UNDEFINED_PROBLEM)) {
                Log.e("LoginState", "iLogin failed, unknown problem");
                setState(LOGGED_OUT);
            } else {
                Log.i("LoginState", "iLogin result: " + result);
                mUser = user;
                appendLog(String.format("Logged in as %s", mUser));
                setState(LOGGED_IN);
            }
        }

        if (mIn == LOGGED_IN) {
            SharedPreferences prefs = ctx.getSharedPreferences("AchSoPrefs", 0);
            if (prefs.getBoolean("autologin", false)) {
                Editor edit = prefs.edit();
                edit.putString("login", user);
                edit.putString("pwd", pass);
                edit.apply();
            }
            return true;
        } else if (mIn == LOGGED_OUT) {
            return false;
        } else {
            Log.e("LoginState", "Still trying to log in, even as task has ended. Strange.");
        }
        return false;
    }

    public String getUser() {
        return mUser;
    }

    public int getState() {
        return mIn;
    }

    private void setState(int state) {
        if (state == LOGGED_OUT) {
            mUser = null;
        } else if (state == LOGGED_IN && state != mIn) {
            Toast.makeText(ctx, "Logged in as " + mUser, Toast.LENGTH_SHORT).show();
            disable_autologin_for_session = true;
        }
        mIn = state;
        Log.i("LoginState", "state set to " + state + ". Should update the icon next. ");
        ((ActionbarActivity) mHost).updateLoginMenuItem();
    }

    public boolean isIn() {
        return (mIn == LOGGED_IN);
    }

    public void logout() {
        mUser = null;
        setState(LOGGED_OUT);
    }

    private class LoginTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... arg) {
            String ret, user, pass;
            if (arg.length < 2) {
                return null;
            }
            user = arg[0];
            pass = arg[1];
            Log.i("LoginTask", "Starting background logging in");
            // create the connection; the result will be a session id or an error message
            LasConnection con = LasConnection.getConnection();
            ret = con.connect(user, pass);
            con.disconnect();
            mUser = user;
            Log.i("LoginTask", "Received logging in response: " + ret);
            return ret;
        }

        protected void onPostExecute(String result) {
            Log.i("LoginTask", "doing onPostExecute with result " + result);
            if (result.equals(LasConnection.CONNECTION_PROBLEM)) {
                Toast.makeText(ctx, "Connection problem", Toast.LENGTH_LONG).show();
                Log.e("LoginState", "Login failed, connection problem");
                setState(LOGGED_OUT);
            } else if (result.equals(LasConnection.AUTHENTICATION_PROBLEM)) {
                Toast.makeText(ctx, "Authentication problem", Toast.LENGTH_LONG).show();
                Log.e("LoginState", "Login failed, authentication problem");
                setState(LOGGED_OUT);
            } else if (result.equals(LasConnection.UNDEFINED_PROBLEM)) {
                Toast.makeText(ctx, "Unknown problem connecting", Toast.LENGTH_LONG).show();
                Log.e("LoginState", "Login failed, unknown problem");
                setState(LOGGED_OUT);
            } else {
                Log.i("LoginState", "Login result: " + result);
                setState(LOGGED_IN);
            }
        }
    }
}