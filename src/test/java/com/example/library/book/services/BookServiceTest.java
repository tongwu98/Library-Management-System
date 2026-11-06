package com.example.library.book.services;

import com.example.library.book.dto.CreateBookRequest;
import com.example.library.book.exceptions.BookConflictException;
import com.example.library.book.exceptions.BookNotFoundException;
import com.example.library.book.models.Book;
import com.example.library.book.repositories.InMemoryBookRepository;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class BookServiceTest {

	private BookService service;

	@BeforeEach
	void setUp() {
		service = new BookService(new InMemoryBookRepository());
	}

	@Test
	void givenPaddedDetails_whenAddingBook_thenStoresTrimmedAvailableBook() {
		Book book = service.add(new CreateBookRequest("  Crescendo Italian Cuisine Menu  ", "  Cristian  "));

		assertThat(book.id()).isPositive();
		assertThat(book.title()).isEqualTo("Crescendo Italian Cuisine Menu");
		assertThat(book.author()).isEqualTo("Cristian");
		assertThat(book.available()).isTrue();
		assertThat(service.get(book.id())).isEqualTo(book);
	}

	@Test
	void givenEmptyLibrary_whenListingBooks_thenReturnsEmptyList() {
		assertThat(service.list(null)).isEmpty();
	}

	@Test
	void givenDuplicateTitles_whenAddingBooks_thenStoresSeparateCopies() {
		Book first = addItalianMenuBook();
		Book second = addItalianMenuBook();

		assertThat(second.id()).isGreaterThan(first.id());
		assertThat(service.list(null)).containsExactlyInAnyOrder(first, second);
	}

	@ParameterizedTest
	@ValueSource(strings = { "ITALIAN", "crisTIAN", "  italian  ", "talian", "Crist" })
	void givenMatchingTitleOrAuthor_whenSearching_thenMatchesIgnoringCase(String query) {
		Book italianMenu = addItalianMenuBook();
		service.add(new CreateBookRequest("Crescendo Dutch Cuisine Menu", "Yemoe"));

		assertThat(service.list(query)).containsExactly(italianMenu);
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { "   " })
	void givenBlankQuery_whenSearching_thenListsEveryBook(String query) {
		Book book = addItalianMenuBook();

		assertThat(service.list(query)).containsExactly(book);
	}

	@Test
	void givenUnmatchedQuery_whenSearching_thenReturnsEmptyList() {
		addItalianMenuBook();

		assertThat(service.list("unknown")).isEmpty();
	}

	@Test
	void givenAvailableBook_whenBorrowedAndReturned_thenPreservesDetailsAndUpdatesStorage() {
		Book original = addItalianMenuBook();

		Book borrowed = service.borrow(original.id());
		assertThat(borrowed).isEqualTo(new Book(original.id(), original.title(), original.author(), false));
		assertThat(service.get(original.id())).isEqualTo(borrowed);
		assertThat(service.list(null)).containsExactly(borrowed);
		assertThat(original.available()).isTrue();

		Book returned = service.returnBook(original.id());
		assertThat(returned).isEqualTo(original);
		assertThat(service.get(original.id())).isEqualTo(returned);
	}

	@Test
	void givenBorrowedBook_whenBorrowingAgain_thenRejectsWithoutChangingAvailability() {
		Book book = addItalianMenuBook();
		service.borrow(book.id());

		assertThatExceptionOfType(BookConflictException.class).isThrownBy(() -> service.borrow(book.id()))
			.withMessageContaining("already borrowed");
		assertThat(service.get(book.id()).available()).isFalse();
	}

	@Test
	void givenAvailableBook_whenReturning_thenRejectsWithoutChangingAvailability() {
		Book book = addItalianMenuBook();

		assertThatExceptionOfType(BookConflictException.class).isThrownBy(() -> service.returnBook(book.id()))
			.withMessageContaining("not currently borrowed");
		assertThat(service.get(book.id()).available()).isTrue();
	}

	@Test
	void givenAvailableBook_whenRemoving_thenDeletesBookWithoutReusingId() {
		Book first = addItalianMenuBook();
		service.remove(first.id());

		assertThat(service.list(null)).isEmpty();
		assertThatExceptionOfType(BookNotFoundException.class).isThrownBy(() -> service.get(first.id()));
		assertThat(addItalianMenuBook().id()).isGreaterThan(first.id());
	}

	@Test
	void givenBorrowedBook_whenRemoving_thenRejectsUntilBookIsReturned() {
		Book book = addItalianMenuBook();
		service.borrow(book.id());

		assertThatExceptionOfType(BookConflictException.class).isThrownBy(() -> service.remove(book.id()))
			.withMessage("Borrowed books cannot be removed");
		assertThat(service.get(book.id()).available()).isFalse();

		service.returnBook(book.id());
		service.remove(book.id());
		assertThat(service.list(null)).isEmpty();
	}

	@Test
	void givenMissingBook_whenGettingBorrowingReturningOrRemoving_thenThrowsNotFound() {
		assertThatExceptionOfType(BookNotFoundException.class).isThrownBy(() -> service.get(42))
			.withMessageContaining("42");
		assertThatExceptionOfType(BookNotFoundException.class).isThrownBy(() -> service.borrow(42));
		assertThatExceptionOfType(BookNotFoundException.class).isThrownBy(() -> service.returnBook(42));
		assertThatExceptionOfType(BookNotFoundException.class).isThrownBy(() -> service.remove(42));
	}

	@Test
	void givenListedBooks_whenLibraryChanges_thenSnapshotRemainsImmutableAndUnchanged() {
		Book book = addItalianMenuBook();
		List<Book> snapshot = service.list(null);
		service.borrow(book.id());
		addItalianMenuBook();

		assertThat(snapshot).containsExactly(book);
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(snapshot::clear);
	}

	private Book addItalianMenuBook() {
		return service.add(new CreateBookRequest("Crescendo Italian Cuisine Menu", "Cristian"));
	}

}
