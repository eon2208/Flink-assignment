package org.enricher.operators.config;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;

public abstract class WireMockConfig {

    private static final WireMockServer wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().port(1234));

    @BeforeAll
    static void wireMockBeforeAll() {
        wireMockServer.start();
    }

    @AfterAll
    static void wireMockAfterAll() {
        wireMockServer.stop();
    }

    @BeforeEach
    void resetWireMockRequests() {
        wireMockServer.resetAll();
    }

    protected void shouldMockTheServiceResponseForValues(int... values) {
        for (int value : values) {
            shouldMockTheServiceResponseForValue(value);
        }
    }

    protected void shouldMockTheServiceResponseForValue(int value) {
        wireMockServer.stubFor(get(urlPathMatching("/value/" + value))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(String.format(
                                """
                                        {
                                          "someIntData1": %d,
                                          "someIntData2": %d,
                                          "someStringData1": "%d",
                                          "someStringData2": "%d"
                                        }
                                        """, value, value, value, value))
                ));
    }

    protected void shouldMockClientErrorForValue(int value) {
        wireMockServer.stubFor(get(urlPathMatching("/value/" + value))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withBody("Not Found")
                ));
    }

    protected void shouldMockServerErrorForValue(int value) {
        wireMockServer.stubFor(get(urlPathMatching("/value/" + value))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")
                ));
    }

    protected void shouldMockUnexpectedStatusCodeForValue(int value) {
        wireMockServer.stubFor(get(urlPathMatching("/value/" + value))
                .willReturn(aResponse()
                        .withStatus(302)
                        .withHeader("Location", "/new-location")
                ));
    }

    protected String getBaseUrl() {
        return wireMockServer.baseUrl();
    }
}
