package com.example.library.book.services;

import com.example.library.book.dto.CreateBookRequest;
import com.example.library.book.exceptions.BookConflictException;
import com.example.library.book.exceptions.BookNotFoundException;
import com.example.library.book.models.Book;
import com.example.library.book.repositories.InMemoryBookRepository;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

@Service
public class BookService {

	private final InMemoryBookRepository repository;

	public BookService(InMemoryBookRepository repository) {
		this.repository = repository;
	}

	public Book add(CreateBookRequest request) {
		return repository.add(request.getTitle().strip(), request.getAuthor().strip());
	}

	public List<Book> list(String query) {
		String search = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
		return repository.findAll()
			.stream()
			.filter(book -> book.title().toLowerCase(Locale.ROOT).contains(search)
					|| book.author().toLowerCase(Locale.ROOT).contains(search))
			.toList();
	}

	public Book get(long id) {
		return repository.findById(id).orElseThrow(() -> new BookNotFoundException(id));
	}

	public void remove(long id) {
		Book book = get(id);
		if (!book.available()) {
			throw new BookConflictException("Borrowed books cannot be removed");
		}
		repository.delete(id);
	}

	public Book borrow(long id) {
		Book book = get(id);
		if (!book.available()) {
			throw new BookConflictException("Book " + id + " is already borrowed");
		}
		Book borrowed = new Book(book.id(), book.title(), book.author(), false);
		repository.save(borrowed);
		return borrowed;
	}

	public Book returnBook(long id) {
		Book book = get(id);
		if (book.available()) {
			throw new BookConflictException("Book " + id + " is not currently borrowed");
		}
		Book returned = new Book(book.id(), book.title(), book.author(), true);
		repository.save(returned);
		return returned;
	}

}
