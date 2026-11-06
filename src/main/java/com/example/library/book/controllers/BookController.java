package com.example.library.book.controllers;

import com.example.library.book.dto.CreateBookRequest;
import com.example.library.book.dto.BookResponse;
import com.example.library.book.models.Book;
import com.example.library.book.services.BookService;
import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/books")
class BookController {

	private final BookService service;

	BookController(BookService service) {
		this.service = service;
	}

	@PostMapping
	ResponseEntity<BookResponse> add(@Valid @RequestBody CreateBookRequest request) {
		Book book = service.add(request);
		return ResponseEntity.created(URI.create("/api/books/" + book.id())).body(toResponse(book));
	}

	@GetMapping
	List<BookResponse> list(@RequestParam(name = "q", required = false) String query) {
		return service.list(query).stream().map(BookController::toResponse).toList();
	}

	@GetMapping("/{id}")
	BookResponse get(@PathVariable long id) {
		return toResponse(service.get(id));
	}

	@DeleteMapping("/{id}")
	ResponseEntity<Void> remove(@PathVariable long id) {
		service.remove(id);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{id}/borrow")
	BookResponse borrow(@PathVariable long id) {
		return toResponse(service.borrow(id));
	}

	@PostMapping("/{id}/return")
	BookResponse returnBook(@PathVariable long id) {
		return toResponse(service.returnBook(id));
	}

	private static BookResponse toResponse(Book book) {
		return new BookResponse(book.id(), book.title(), book.author(), book.available());
	}

}
