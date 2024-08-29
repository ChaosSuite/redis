package cc.yylives.chaossuite.redis.spring_test;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.time.Duration;


class SpringTestApplicationTests {

	private static final String URL = "http://127.0.0.1:8080/redis/get?key=test";
    private static final String EXPECTED_RESPONSE = "test1";
    private static final long TEST_DURATION_SECONDS = 30;
    private static final long MAX_FAILURE_DURATION_SECONDS = 15;

	@Test
    public void testRedisEndpoint() throws Exception {
        Instant startTime = Instant.now();
        long failureDuration = 0;
        boolean testPassed = true;

        while (Duration.between(startTime, Instant.now()).getSeconds() < TEST_DURATION_SECONDS) {
            try {
                String response = sendGetRequest(URL);
                if (!EXPECTED_RESPONSE.equals(response)) {
                    Instant failureStart = Instant.now();
                    while (!EXPECTED_RESPONSE.equals(sendGetRequest(URL))) {
						long duration = Duration.between(startTime, Instant.now()).getSeconds();
						System.out.println("Failed: " + duration + " seconds");
                        if (Duration.between(failureStart, Instant.now()).getSeconds() > MAX_FAILURE_DURATION_SECONDS) {
                            testPassed = false;
                            break;
                        }
                        Thread.sleep(1000); // Wait for 1 second before retrying
                    }
                    failureDuration = Duration.between(failureStart, Instant.now()).getSeconds();
                    if (!testPassed) break;
                }else{
					long duration = Duration.between(startTime, Instant.now()).getSeconds();
                    System.out.println("OK: " + duration + " seconds");
				}
            } catch (Exception e) {
                System.out.println("Error occurred: " + e.getMessage());
				long duration = Duration.between(startTime, Instant.now()).getSeconds();
				System.out.println("Failed: " + duration + " seconds");
            }
            Thread.sleep(1000); // Wait for 1 second before the next request
        }

        assertTrue(testPassed, "Test failed. Failure duration: " + failureDuration + " seconds");
        System.out.println("Test passed. Maximum failure duration: " + failureDuration + " seconds");
    }

    private String sendGetRequest(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setConnectTimeout(3000);
        con.setReadTimeout(3000);

        int status = con.getResponseCode();
        if (status == 200) {
            try (java.util.Scanner scanner = new java.util.Scanner(con.getInputStream()).useDelimiter("\\A")) {
                return scanner.hasNext() ? scanner.next() : "";
            }
        } else {
			return "Failed";
            // throw new RuntimeException("HTTP error code: " + status);
        }
    }
}
