package org.enricher.operator.utils;

import com.github.tomakehurst.wiremock.WireMockServer;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class ServiceWireMockUtils {

    public static void shouldMockTheServiceResponseForValue(WireMockServer wireMockServer, int value) {
        wireMockServer.removeStub(get(urlMatching("/value/\\d+")));
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

    public static void shouldMockClientErrorForValue(WireMockServer wireMockServer, int value) {
        wireMockServer.stubFor(get(urlPathMatching("/value/" + value))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withBody("Not Found")
                ));
    }

    public static void shouldMockServerErrorForValue(WireMockServer wireMockServer, int value) {
        wireMockServer.stubFor(get(urlPathMatching("/value/" + value))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")
                ));
    }

    public static void shouldMockUnexpectedStatusCodeForValue(WireMockServer wireMockServer, int value) {
        wireMockServer.stubFor(get(urlPathMatching("/value/" + value))
                .willReturn(aResponse()
                        .withStatus(302)
                        .withHeader("Location", "/new-location")
                ));
    }
}
