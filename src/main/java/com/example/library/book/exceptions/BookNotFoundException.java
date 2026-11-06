package com.example.library.book.exceptions;

public class BookNotFoundException extends RuntimeException {

	public BookNotFoundException(long id) {
		super("Book " + id + " was not found");
	}

}
