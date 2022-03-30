package com.kmcaps.jbk.androidreader;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class PapagoTrans extends Thread {
    final String clientId = "ieocAf49DYEdpxkPoSKF";//애플리케이션 클라이언트 아이디값";
    final String clientSecret = "d3M9jAoNPk";//애플리케이션 클라이언트 시크릿값";
    String resultText = "";
    String source = "";
    String target = "";
    String texts = "";

    public PapagoTrans(String texts, String source, String target) {
        this.texts = texts;
        this.source = source;
        this.target = target;
    }

    public void run() {
        try {
            String text = URLEncoder.encode(texts, "UTF-8");
            String apiURL = "https://openapi.naver.com/v1/papago/n2mt";
            URL url = new URL(apiURL);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("X-Naver-Client-Id", clientId);
            con.setRequestProperty("X-Naver-Client-Secret", clientSecret);
            // post request
            String postParams = "source=" + source + "&target=" + target + "&text=" + text;
            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(postParams);
            wr.flush();
            wr.close();
            int responseCode = con.getResponseCode();
            BufferedReader br;
            if (responseCode == 200) { // 정상 호출
                br = new BufferedReader(new InputStreamReader(con.getInputStream()));
            } else {  // 에러 발생
                br = new BufferedReader(new InputStreamReader(con.getErrorStream()));
            }
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = br.readLine()) != null) {
                response.append(inputLine);
            }
            br.close();

            JSONObject jo = new JSONObject(response.toString());
            String translatedText = jo.getJSONObject("message").getJSONObject("result").getString("translatedText");
            resultText = translatedText;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getResultText(){
        return this.resultText;
    }
}
