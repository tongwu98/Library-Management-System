package com.example.library.book.repositories;

import com.example.library.book.models.Book;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Repository;

@Repository
public class InMemoryBookRepository {

	private final Map<Long, Book> books = new HashMap<>();

	private long nextId = 1;

	public Book add(String title, String author) {
		Book book = new Book(nextId++, title, author, true);
		books.put(book.id(), book);
		return book;
	}

	public List<Book> findAll() {
		return List.copyOf(books.values());
	}

	public Optional<Book> findById(long id) {
		return Optional.ofNullable(books.get(id));
	}

	public void save(Book book) {
		books.put(book.id(), book);
	}

	public void delete(long id) {
		books.remove(id);
	}

}
