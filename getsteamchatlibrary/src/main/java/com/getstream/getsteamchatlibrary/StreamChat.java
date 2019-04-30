package com.getstream.getsteamchatlibrary;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.getstream.getsteamchatlibrary.Signing.JWTUserToken;
import static com.getstream.getsteamchatlibrary.Signing.UserFromToken;
import static com.getstream.getsteamchatlibrary.Signing.encodeBase64;

public class StreamChat {

    public static User getCurrentUser() {
        return user;
    }

    interface MyCallBackInterface {
        void onSuccess(String result);
        void onFailure(String error, int nCode);
    }

    static String key;
    String secret;
    static String userToken;
    ClientState state;
    static public String baseURL, wsBaseURL, wsURL,clientID;
    boolean anonymous, connecting;
    String UUID_,userID;
    int failures = 0;
    static User user;
    static ArrayList<Channel> activeChannels;
    public StableWSConnection wsConnection;

    public StreamChat(String key, String secretOrToptions, String options) {

        // Set the key
        this.key = key;
        this.secret = null;
        this.state = new ClientState();

        activeChannels = new ArrayList<Channel>();
        // set the secret

//        this.browser =
//                typeof options.browser !== 'undefined'
//                ? options.browser
//                : typeof window !== 'undefined';
//        this.node = !this.browser;
//
//		const defaultOptions = {
//                timeout: 3000,
//		};


        FormBody.Builder defaultOptions = new FormBody.Builder()
                .add("timeout", "3000");

//        RequestBody formBody = defaultOptions.build();




        this.setBsaeURL("https://chat-us-east-1.stream-io-api.com");

    }

    public Channel channel(String channelType, String channelID, String name,String image,ArrayList<Member> members, int session){

        Channel channel = null;
        if(channelID.length()>0){
            String cid = channelType + ":" + channelID;
            for(int i=0;i<this.activeChannels.size();i++){
                if(this.activeChannels.get(i).cid.equals(cid)){
                    channel = this.activeChannels.get(i);

                    return channel;
                }
            }
            channel = new Channel(this,channelType,channelID,name,image,members,session);
            this.activeChannels.add(channel);
        }else{

            channel = new Channel(this,channelType,"",name,image,members,session);

        }
        return channel;

    }

    public void setUser(User user, String userToken){

        this.userToken = userToken;

        this.userID = user.id;
        if(userToken == null && this.secret != null){
            this.userToken = this.createToken(user.id);
        }

        if(this.userToken == null){
            return;
        }

        String tokenUserId = UserFromToken(this.userToken);

        this._setUser(user);
        this.anonymous = false;


        this._setupConnection();
    }

    void _setupConnection() {

        this.UUID_ = String.valueOf(UUID.randomUUID());
        this.clientID = this.user.id + "--" + this.UUID_;
        this.connect();
    }
    void connect() {
        this.connecting = true;
        this.failures = 0;

//        if(client.userID == null){
//            return;
//        }


//add items

        String client_id = "\"client_id\":\""+ this.clientID + "\"";
        String user_id = "\"user_id\":\""+ this.userID + "\"";
        String userString = "\"" +"id\":\"" + this.userID + "\"" + ",\"name\":" + "\"" + this.user.name + "\"" + ",\"image\":" + "\"" + this.user.image + "\"";
        String user_details = "\"user_details\":{" + userString + "}";
        String user_token = "\"user_token\":\"" + this.userToken + "\"";

        String strqs = "{" + client_id +"," + user_id +"," + user_details +"," + user_token +"}";
        String qs = URLEncoder.encode(strqs);
        if(qs.length() > 1900)
            return;

        String token = "";
        if(this.anonymous == false){
            token = this.userToken != null ? this.userToken : JWTUserToken("",user.id,"","");
        }

        String authType = this.getAuthType();

        this.wsURL = this.wsBaseURL + "/connect?json=" + qs + "&api_key="
                + this.key + "&authorization=" + token + "&stream-auth-type=" + authType;

        wsConnection = new StableWSConnection(wsURL, clientID, user.id/*, this.recoverState, this.handleEvent, this.dispatchEvent*/);

        wsConnection.connect();

    }

//    void connect() {
//        this.connecting = true;
//        this.failures = 0;
//
////        if(client.userID == null){
////            return;
////        }
//
//
////add items
//
////        String client_id = "\"client_id\":\""+ this.clientID + "\"";
//        String user_id = "\"user_id\":\""+ this.userID + "\"";
//        String userString = "\"" +"id\":\"" + this.userID + "\"" + ",\"name\":" + "\"" + this.user.name + "\"" + ",\"image\":" + "\"" + this.user.image + "\"";
//        String user_details = "\"user_details\":{" + userString + "}";
//        String user_token = "\"user_token\":\"" + this.userToken + "\"";
//        String server_determines_connection_id = "\"server_determines_connection_id\":\"true\"";
//
//        String strqs = "{"+ server_determines_connection_id + "," + user_id +"," + user_details + "}";
//        String qs = URLEncoder.encode(strqs);
//		if(qs.length() > 1900)
//            return;
//
//        String token = "";
//        if(this.anonymous == false){
//            token = this.userToken != null ? this.userToken : JWTUserToken("",user.id,"","");
//        }
//
//        String authType = this.getAuthType();
//
//        this.wsURL = this.wsBaseURL + "/connect?json=" + qs + "&api_key="
//                + this.key + "&authorization=" + token + "&stream-auth-type=" + authType;
//
////        this.wsURL = this.wsBaseURL + "/connect?json=" + qs + "&api_key="
////                + this.key + "&stream-auth-type=" + authType;
//
//        wsConnection = new StableWSConnection(wsURL, clientID, user.id/*, this.recoverState, this.handleEvent, this.dispatchEvent*/);
//
//        wsConnection.connect();
//
//    }


    String getAuthType() {
        return this.anonymous ? "anonymous" : "jwt";
    }

    void _setUser(User user){
        this.user = user;
    }

    String createToken(String userID){

        return JWTUserToken("v4dg6xc6kr6ygsvb2ej5j953ybjqddc9pjgvdqh6suag6hyhr2ezfctq6ez62qhq",userID,"1","1");

    }

    void setBsaeURL(String baseURL) {
        this.baseURL = baseURL;
        this.wsBaseURL = this.baseURL.replace("http","ws");
    }

    public void createChannelType(String data){
        FormBody.Builder formBuilder = new FormBody.Builder()
                .add("name", this.UUID_);
        RequestBody formBody = formBuilder.build();

        APIManager.getInstance().post(baseURL + "/channeltypes", formBody, new APIManager.MyCallBackInterface() {
            @Override
            public void onSuccess(String result) {

            }

            @Override
            public void onFailure(final String error, int nCode) {

            }
        });
    }

    void getChannelType(String channelType){

        APIManager.getInstance().get(this.baseURL+"/channeltypes/" + channelType, new APIManager.MyCallBackInterface() {
            @Override
            public void onSuccess(String result) {

            }

            @Override
            public void onFailure(final String error, int nCode) {

            }
        });
    }

    void post(String url, String params){
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(JSON, params);



        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Authorization", this.userToken)
                .addHeader("Content-Type","application/json")//Notice this request has header if you don't need to send a header just erase this part
                .addHeader("Stream-Auth-Type","jwt")
                .build();

        Call call = client.newCall(request);

        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

            }

        });

    }
    static public void queryChannels(final MyCallBackInterface callback){

        JSONArray memArray = new JSONArray();
        memArray.put(user.id);
//        for(int i=0;i<members.size();i++){
//            memArray.put(members.get(i).user.id);
//        }

        String queryChannels_url = baseURL + "/channels" + "?api_key=" +key + "&payload={\"filter_conditions\":{\"members\":{\"$in\":" + memArray.toString() + "}},\"sort\":[{\"field\":\"last_message_at\",\"direction\":-1}],\"state\":true,\"watch\":true}"  + "&client_id=" + clientID;
        get(queryChannels_url, new MyCallBackInterface() {
            @Override
            public void onSuccess(String result) {

                callback.onSuccess(result);
            }

            @Override
            public void onFailure(String error, int nCode) {

                callback.onFailure(error,nCode);
            }
        });

    }
    static void get(String url, final MyCallBackInterface callback){
        final MediaType JSON = MediaType.parse("application/json; charset=utf-8");



        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", userToken)
                .addHeader("Content-Type","application/json")//Notice this request has header if you don't need to send a header just erase this part
                .addHeader("Stream-Auth-Type","jwt")
                .build();

        Call call = client.newCall(request);

        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                int code = response.code();
                if(code == 200){
                    String jsonData = response.body().string();


                    activeChannels.clear();
                    try {
                        JSONObject jsonObject = new JSONObject(jsonData);
                        JSONArray channelArray = jsonObject.getJSONArray("channels");

                        for(int i = 0;i < channelArray.length();i++){
                            JSONObject jsonObj = channelArray.getJSONObject(i);
                            Channel channel = new JSONParser().parseChannelData(jsonObj);
                            activeChannels.add(channel);
                        }


                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    callback.onSuccess(jsonData);
                }else{
                    callback.onFailure("error",code);
                }

            }

        });

    }
}
