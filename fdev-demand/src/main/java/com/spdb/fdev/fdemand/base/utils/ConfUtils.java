package com.spdb.fdev.fdemand.base.utils;

import com.spdb.fdev.fdemand.spdb.dto.conf.ConfDto;
import com.spdb.fdev.fdemand.spdb.dto.conf.ContentDto;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import sun.misc.BASE64Encoder;

import java.io.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Component
@RefreshScope
public class ConfUtils {

    @Value("${conf.url}")
    private String confUrl;
//    private String confUrl = "http://conf.spdb.com:8090";

    @Value("${conf.space.key}")
    private String confSpaceKey;

    @Value("${conf.account.username}")
    private String username;

    @Value("${conf.account.password}")
    private String password;

    @Autowired
    private RestTemplate restTemplate;

    private HttpEntity<Object> getHttpEntity() {
        return new HttpEntity<>(getHttpHeaders());
    }

    private HttpEntity<Object> getHttpEntity(Object body) {
        return new HttpEntity<>(body, getHttpHeaders());
    }

    private HttpHeaders getHttpHeaders() {
        return new HttpHeaders() {{
            try {
                String auth = "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes("UTF-8"));
                add("Authorization", auth);
                add("Content-Type", "application/json");
                // setBasicAuth(username, password, );
                // setContentType(MediaType.APPLICATION_JSON);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }};
    }

    public String getDefaultSpaceUrl() {
        String[] confSpaceKeyArray = confSpaceKey.split(",");//???????????????????????????
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        for (int i = 0; i < confSpaceKeyArray.length; i++) {
            sb.append("space=");
            sb.append(confSpaceKeyArray[i]);
            if (i != confSpaceKeyArray.length - 1) {
                sb.append(" or ");
            }
        }
        sb.append(")");
        return sb.toString();
    }

    public ConfDto sendGet(String url) {
        return restTemplate.exchange(url, HttpMethod.GET, getHttpEntity(), ConfDto.class).getBody();
    }

    public ConfDto sendPost(String url, Object body) {
        return restTemplate.postForObject(url, getHttpEntity(body), ConfDto.class);
    }

    public List<ContentDto> getTotalPageData(String paramUrl) {
        List<ContentDto> result = new ArrayList<>();
        int limit = 500;//????????????????????????500???????????????????????? ?????????????????????1000???????????????1000??????????????????size?????????500???
        int page = 0;
        int size;
        do {//??????limit==size????????????????????????????????????
            page++;
            int start = (page - 1) * limit;
            String url = getConfApi() + paramUrl + ('?' == paramUrl.charAt(paramUrl.length() - 1) ? "" : "&") + "start=" + start + "&limit=" + limit;//????????????500
            ConfDto body = sendGet(url);
            result.addAll(body.getResults());
            size = body.getSize();
        } while (limit == size);
        return result;
    }

    public String getConfApi() {
        return confUrl + "/rest/api";
    }

    public String getConfUrl() {
        return confUrl;
    }

    public void addLabel(String pageId) {
        CloseableHttpClient closeableHttpClient = HttpClients.createDefault();
        CloseableHttpResponse closeableHttpResponse = null;
        String content = "[{\"prefix\": \"global\",\"name\": \"?????????\"}]";//??????json?????????{"name":"zhans"}
        String url = getConfApi() + "/content/" + pageId + "/label";//???????????????http://conf.spdb.com:8090/rest/api/content/139105640/label
        try {
            //?????????????????????
            BASE64Encoder encoder = new BASE64Encoder();
            String userPwd = username + ":" + password;
            String encoding = encoder.encode(userPwd.getBytes("UTF-8"));
            //??????json
            StringEntity stringEntity = new StringEntity(content, "UTF-8");
            stringEntity.setContentEncoding("utf-8");
            stringEntity.setContentType("application/json");
            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("Authorization", "Basic " + encoding);
            httpPost.setEntity(stringEntity);
            closeableHttpResponse = closeableHttpClient.execute(httpPost);
            //????????????????????????closeableHttpResponse.getStatusLine().getStatusCode()??????????????????????????????200???500??????
            String result = EntityUtils.toString(closeableHttpResponse.getEntity(), "UTF-8");
            System.out.println(result);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (closeableHttpResponse != null) {
                try {
                    closeableHttpResponse.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public byte[] exportWord(String pageId, String templateId) throws Exception {
        String url = getConfUrl() + "/plugins/servlet/scroll-office/api/public/1/export-sync?templateId=" + templateId + "&pageId=" + pageId;
        HttpHeaders header = new HttpHeaders();
        List list = new ArrayList();
        list.add(MediaType.APPLICATION_OCTET_STREAM);
        header.setAccept(list);
        ResponseEntity<byte[]> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<byte[]>(header), byte[].class);
        return response.getBody();
    }
}
