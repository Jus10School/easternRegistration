package com.example;

import java.io.IOException;
import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.CookiePolicy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Retrieves Eastern course sections by academic term and subject, using
 * cached JSON responses when available.
 *
 * <p>Cache files are read from the project's {@code cache} directory and
 * fetched responses are written there for later lookups.</p>
 */
public class EasternSchedule {

    private static final String BASE = "https://reg-prod.ec.easternct.edu/StudentRegistrationSsb";
    private static final String DEFAULT_TERM = "202710";
    private final HttpClient client;

    /**
     * Creates a schedule client configured for the default term.
     */
    public EasternSchedule() {
        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);

        client = HttpClient.newBuilder()
            .cookieHandler(cookieManager)
            .build();
    }

    /**
     * Returns the academic term code used as the default selection.
     *
     * @return the default term code
     */
    public String getTerm() {
        return DEFAULT_TERM;
    }

    /**
     * Retrieves available terms from the local cache, or from Eastern if the
     * cache does not exist.
     *
     * @return available academic terms in the order returned by Eastern
     * @throws IOException if term data cannot be read or parsed
     * @throws InterruptedException if the request is interrupted
     */
    public ArrayList<Term> getAvailableTerms() throws IOException, InterruptedException {
        Path cacheFile = getCacheFile("terms.txt");
        String content;
        if (Files.exists(cacheFile)) {
            content = Files.readString(cacheFile);
        } else {
            String url = BASE + "/ssb/classSearch/getTerms?offset=1&max=25&searchTerm=";
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .header("Accept", "application/json")
                .build();
            HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
            );
            if (response.statusCode() != 200) {
                throw new IOException(
                    "Could not retrieve terms: HTTP " + response.statusCode()
                );
            }
            content = response.body();
            Files.createDirectories(cacheFile.getParent());
            Files.writeString(cacheFile, content);
        }

        return new ObjectMapper().readValue(
            content,
            new TypeReference<ArrayList<Term>>() {}
        );
    }

    /**
     * Parses course records from the response's {@code data} array.
     *
     * @param content JSON response content
     * @return parsed course sections
     * @throws JsonProcessingException if the JSON cannot be parsed
     */
    private ArrayList<Course> parseCourses(String content)
        throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode dataNode = mapper.readTree(content).get("data");        
        ArrayList<Course> courses = mapper.convertValue(dataNode, new TypeReference<ArrayList<Course>>() {});
        return courses;
    }

    /**
     * Selects the requested term in Eastern's registration service.
     *
     * @param termCode academic term code to select
     * @throws IOException if Eastern rejects the term selection
     * @throws InterruptedException if the request is interrupted
     */
    private void postTerm(String termCode) throws IOException, InterruptedException {

        String url = BASE + "/ssb/term/search?mode=search";

        String body =
            "term=" + URLEncoder.encode(termCode, StandardCharsets.UTF_8) +
            "&studyPath=" +
            "&studyPathText=" +
            "&startDatepicker=" +
            "&endDatepicker=";

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header(
                "Content-Type",
                "application/x-www-form-urlencoded; charset=UTF-8"
            )            
            .POST(HttpRequest.BodyPublishers.ofString(body))            
            .build();

        HttpResponse<Void> response =
            client.send(request, HttpResponse.BodyHandlers.discarding());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException(
                "Could not select term " + termCode + ": HTTP "
                    + response.statusCode()
            );
        }

    }

    /**
     * Resolves a cache file under the project's cache directory.
     *
     * @param fileName name of the cache file
     * @return path to the cache file
     */
    private Path getCacheFile(String fileName) {
        Path cacheDirectory = Path.of("cache");
        if (!Files.isDirectory(cacheDirectory) && Files.isDirectory(Path.of("..", "cache"))) {
            cacheDirectory = Path.of("..", "cache");
        }
        return cacheDirectory.resolve(fileName);
    }

    /**
     * Fetches and caches courses for the specified term and subject if the
     * corresponding cache file does not already exist.
     *
     * @param termCode academic term code
     * @param subject the subject to cache courses for
     * @throws IOException if the request or cache operation fails
     * @throws InterruptedException if an HTTP request is interrupted
     */
    private void cacheCourses(String termCode, String subject)
        throws IOException, InterruptedException {
        
        Path cacheFile = getCacheFile("term_" + termCode + "_" + subject + ".txt");

        // if file already exists, return
        if (Files.exists(cacheFile)) {            
           return;           
        }

        // post request to establish term is required
        postTerm(termCode);

        String url = BASE + "/ssb/searchResults/searchResults?" +
        "txt_subject=" + subject + "&" +
        "txt_term=" + URLEncoder.encode(termCode, StandardCharsets.UTF_8) + "&" +
        "startDatepicker=&endDatepicker=&pageOffset=0&pageMaxSize=500&sortColumn=subjectDescription&sortDirection=asc";
                                                    
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header(
                "Accept", "application/json"                    
            )              
            .GET()           
            .build();

        HttpResponse<String> response =
            client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException(
                "Course search failed: HTTP " + response.statusCode()
                    + "\n" + response.body()
            );
        }

        // save data to cache file
        Files.createDirectories(cacheFile.getParent());
        Files.writeString(cacheFile, response.body());
    }

    /**
     * Queries course sections by term and subject, using the cache when
     * available and fetching the response otherwise.
     *
     * @param termCode non-blank academic term code
     * @param subject non-blank subject code to retrieve
     * @return matching course sections
     * @throws IOException if the request, cache operation, or JSON parsing fails
     * @throws InterruptedException if an HTTP request is interrupted
     */
    public ArrayList<Course> queryCourseBySubject(String termCode, String subject)
        throws IOException, InterruptedException {
        if (termCode == null || termCode.isBlank()) {
            throw new IllegalArgumentException("termCode must not be blank");
        }
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("subject must not be blank");
        }
        cacheCourses(termCode, subject);
        Path cacheFile = getCacheFile("term_" + termCode + "_" + subject + ".txt");
        String content = Files.readString(cacheFile);

        return parseCourses(content);
    }
}
