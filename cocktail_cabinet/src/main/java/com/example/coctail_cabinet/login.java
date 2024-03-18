package com.example.coctail_cabinet;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.http.HttpEntity;


//네이버 로그인

@RestController
@RequestMapping("/api/auth")
public class login {

    @Value("${oauth.naver.client-id}")
    private String clientId;

    @Value("${oauth.naver.secret}")
    private String clientSecret;

    @Value("${oauth.naver.url.auth}")
    private String naverAuthUrl;

    @GetMapping("/naver")
    public ResponseEntity<String> naverLoginUrl() throws UnsupportedEncodingException {
        String redirectUri = URLEncoder.encode("http://localhost:8080", "UTF-8");
        String state = "RANDOM_STATE";
        String authUrl = naverAuthUrl + "?response_type=code" +
                "&client_id=" + clientId +
                "&redirect_uri=" + redirectUri +
                "&state=" + state;

        return ResponseEntity.ok(authUrl);
    }
    
    @GetMapping("/callback")
    public ResponseEntity<?> naverCallback(@RequestParam String code, @RequestParam String state) {
        // RestTemplate을 사용하여 액세스 토큰 요청
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("code", code);
        params.add("state", state);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        String tokenUrl = "https://nid.naver.com/oauth2.0/token";
        ResponseEntity<String> response = restTemplate.postForEntity(tokenUrl, request, String.class);

        // 액세스 토큰을 사용하여 사용자 정보 요청 등의 로직 구현
        // JSON 응답에서 액세스 토큰 추출
        String responseBody = response.getBody();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = null;
        
        try {
            rootNode = objectMapper.readTree(responseBody);
            // JSON 처리 로직
        } catch (JsonProcessingException e) {
            // 예외 처리 로직
            e.printStackTrace();
        }

        String accessToken = rootNode.path("access_token").asText();

        // 액세스 토큰을 사용하여 사용자 정보 요청
        headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        HttpEntity<?> userInfoRequest = new HttpEntity<>(headers);
        String userInfoUrl = "https://openapi.naver.com/v1/nid/me";
        ResponseEntity<String> userInfoResponse = restTemplate.exchange(
                userInfoUrl, HttpMethod.GET, userInfoRequest, String.class);

        // 사용자 정보 응답 출력
        String userInfoResponseBody = userInfoResponse.getBody();
        System.out.println("사용자 정보 응답: " + userInfoResponseBody);

        try {
            // 사용자 정보 파싱
            JsonNode userInfoRootNode = objectMapper.readTree(userInfoResponseBody);
            JsonNode responseNode = userInfoRootNode.path("response");
            String name = responseNode.path("name").asText();
            String email = responseNode.path("email").asText();
            String nickname = responseNode.path("nickname").asText();
            String profileImage = responseNode.path("profile_image").asText();
            String mobile = responseNode.path("mobile").asText();

            // 터미널에 출력(나중에 mariaDB에 저장되게끔 바꿔주기)
            System.out.println("회원 이름: " + name);
            System.out.println("연락처 이메일 주소: " + email);
            System.out.println("별명: " + nickname);
            System.out.println("프로필 사진 URL: " + profileImage);
            System.out.println("휴대전화 번호: " + mobile);
        } catch (JsonProcessingException e) {
            // 예외 처리 로직
            e.printStackTrace();
        }

        return ResponseEntity.ok(response.getBody());
    }

}