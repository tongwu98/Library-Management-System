package com.example.library.integration;

import com.example.library.book.models.Book;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class BookSteps {

	@LocalServerPort
	private int port;

	@Autowired
	private ObjectMapper mapper;

	private HttpClient client;

	private HttpResponse<String> response;

	private Book savedBook;

	@Before
	public void emptyLibrary() throws IOException, InterruptedException {
		client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
		send("GET", "/api/books", null);
		assertStatus(200);
		// Clean up through the public API rather than adding a production reset endpoint.
		for (Book book : books()) {
			if (!book.available()) {
				send("POST", "/api/books/" + book.id() + "/return", null);
				assertStatus(200);
			}
			send("DELETE", "/api/books/" + book.id(), null);
			assertStatus(204);
		}
	}

	@After
	public void closeClient() {
		if (client != null) {
			client.close();
		}
	}

	@Given("the library contains these books:")
	public void existingBooks(DataTable table) throws IOException, InterruptedException {
		for (Map<String, String> row : table.asMaps()) {
			availableBook(row.get("title"), row.get("author"));
		}
	}

	@Given("an available book titled {string} by {string}")
	public void availableBook(String title, String author) throws IOException, InterruptedException {
		addBook(title, author);
		assertStatus(201);
	}

	@Given("a borrowed book titled {string} by {string}")
	public void borrowedBook(String title, String author) throws IOException, InterruptedException {
		availableBook(title, author);
		borrowBook();
		assertStatus(200);
		savedBook = mapper.readValue(response.body(), Book.class);
	}

	@When("I add a book titled {string} by {string}")
	public void addBook(String title, String author) throws IOException, InterruptedException {
		submitBook(mapper.writeValueAsString(Map.of("title", title, "author", author)));
	}

	@When("I submit the following book:")
	public void submitBook(String json) throws IOException, InterruptedException {
		send("POST", "/api/books", json);
		if (response.statusCode() == 201) {
			savedBook = mapper.readValue(response.body(), Book.class);
		}
	}

	@When("I list the books")
	public void listBooks() throws IOException, InterruptedException {
		send("GET", "/api/books", null);
	}

	@When("I search for {string}")
	public void search(String query) throws IOException, InterruptedException {
		send("GET", "/api/books?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8), null);
	}

	@When("I borrow the book")
	public void borrowBook() throws IOException, InterruptedException {
		send("POST", bookPath() + "/borrow", null);
	}

	@When("I return the book")
	public void returnBook() throws IOException, InterruptedException {
		send("POST", bookPath() + "/return", null);
	}

	@When("I remove the book")
	public void removeBook() throws IOException, InterruptedException {
		send("DELETE", bookPath(), null);
	}

	@When("I send {string} to {string}")
	public void request(String method, String path) throws IOException, InterruptedException {
		send(method, path, null);
	}

	@Then("the book should be created as {string} by {string}")
	public void createdBook(String title, String author) throws IOException, InterruptedException {
		assertStatus(201);
		assertThat(savedBook.id()).isPositive();
		assertThat(response.headers().firstValue("Location")).hasValue(bookPath());
		assertStoredBook(new Book(savedBook.id(), title, author, true));
	}

	@Then("the library should contain these available books:")
	public void listedBooks(DataTable table) {
		assertStatus(200);
		Book[] actual = books();
		var expected = table.asMaps().stream().map(row -> tuple(row.get("title"), row.get("author"), true)).toList();
		assertThat(actual).allSatisfy(book -> assertThat(book.id()).isPositive())
			.extracting(Book::id)
			.doesNotHaveDuplicates();
		assertThat(actual).extracting(Book::title, Book::author, Book::available)
			.containsExactlyInAnyOrderElementsOf(expected);
	}

	@Then("the library should contain {int} books")
	public void listSize(int count) {
		assertStatus(200);
		assertThat(books()).hasSize(count);
	}

	@Then("the search should return only {string}")
	public void searchResult(String title) {
		assertStatus(200);
		assertThat(books()).extracting(Book::title).containsExactly(title);
	}

	@Then("the book should now be borrowed")
	public void bookBorrowed() throws IOException, InterruptedException {
		assertStatus(200);
		assertStoredBook(new Book(savedBook.id(), savedBook.title(), savedBook.author(), false));
	}

	@Then("the book should now be available")
	public void bookReturned() throws IOException, InterruptedException {
		assertStatus(200);
		assertStoredBook(new Book(savedBook.id(), savedBook.title(), savedBook.author(), true));
	}

	@Then("the book should no longer exist")
	public void bookRemoved() throws IOException, InterruptedException {
		assertStatus(204);
		assertThat(response.body()).isEmpty();
		send("GET", bookPath(), null);
		assertProblem(404);
		listBooks();
		listSize(0);
	}

	@Then("the request should fail with status {int}")
	public void requestFailure(int status) throws IOException, InterruptedException {
		assertProblem(status);
		assertSavedBookUnchanged();
	}

	@Then("the request should fail with status {int} and message {string}")
	public void requestFailureWithMessage(int status, String message) throws IOException, InterruptedException {
		assertProblem(status);
		assertThat(body().path("detail").asString()).contains(message);
		assertSavedBookUnchanged();
	}

	@Then("the request should fail validation for {string}")
	public void validationFailure(String field) throws IOException, InterruptedException {
		assertProblem(400);
		assertThat(body().path("errors").path(field).asString()).isNotBlank();
		listBooks();
		listSize(0);
	}

	@Then("the published OpenAPI contract should be returned")
	public void publishedContract() throws IOException {
		assertStatus(200);
		assertThat(response.body())
			.isEqualTo(new ClassPathResource("static/openapi.yaml").getContentAsString(StandardCharsets.UTF_8));
	}

	private void assertStoredBook(Book expected) throws IOException, InterruptedException {
		assertThat(mapper.readValue(response.body(), Book.class)).isEqualTo(expected);
		send("GET", bookPath(), null);
		assertStatus(200);
		assertThat(mapper.readValue(response.body(), Book.class)).isEqualTo(expected);
	}

	private void assertSavedBookUnchanged() throws IOException, InterruptedException {
		if (savedBook != null) {
			send("GET", bookPath(), null);
			assertStatus(200);
			assertThat(mapper.readValue(response.body(), Book.class)).isEqualTo(savedBook);
		}
	}

	private void assertStatus(int status) {
		assertThat(response.statusCode()).as("HTTP response: %s", response.body()).isEqualTo(status);
	}

	private void assertProblem(int status) {
		assertStatus(status);
		assertThat(response.headers().firstValue("Content-Type"))
			.hasValueSatisfying(value -> assertThat(value).startsWith("application/problem+json"));
		assertThat(body().path("status").intValue()).isEqualTo(status);
		assertThat(body().path("detail").asString()).isNotBlank();
	}

	private void send(String method, String path, String json) throws IOException, InterruptedException {
		HttpRequest.Builder request = HttpRequest.newBuilder()
			.uri(URI.create("http://localhost:" + port + path))
			.timeout(Duration.ofSeconds(10));
		if (json != null) {
			request.header("Content-Type", "application/json");
		}
		request.method(method,
				json == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(json));
		response = client.send(request.build(), HttpResponse.BodyHandlers.ofString());
	}

	private JsonNode body() {
		return mapper.readTree(response.body());
	}

	private Book[] books() {
		return mapper.readValue(response.body(), Book[].class);
	}

	private String bookPath() {
		return "/api/books/" + savedBook.id();
	}

}
