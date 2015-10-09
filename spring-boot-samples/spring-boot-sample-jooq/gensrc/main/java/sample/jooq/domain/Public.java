/**
 * This class is generated by jOOQ
 */
package sample.jooq.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Generated;

import org.jooq.Table;
import org.jooq.impl.SchemaImpl;

import sample.jooq.domain.tables.Author;
import sample.jooq.domain.tables.Book;
import sample.jooq.domain.tables.BookStore;
import sample.jooq.domain.tables.BookToBookStore;
import sample.jooq.domain.tables.Language;

/**
 * This class is generated by jOOQ.
 */
@Generated(value = { "http://www.jooq.org",
		"jOOQ version:3.6.2" }, comments = "This class is generated by jOOQ")
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Public extends SchemaImpl {

	private static final long serialVersionUID = 1051839433;

	/**
	 * The reference instance of <code>PUBLIC</code>
	 */
	public static final Public PUBLIC = new Public();

	/**
	 * No further instances allowed
	 */
	private Public() {
		super("PUBLIC");
	}

	@Override
	public final List<Table<?>> getTables() {
		List result = new ArrayList();
		result.addAll(getTables0());
		return result;
	}

	private final List<Table<?>> getTables0() {
		return Arrays.<Table<?>>asList(Language.LANGUAGE, Author.AUTHOR, Book.BOOK,
				BookStore.BOOK_STORE, BookToBookStore.BOOK_TO_BOOK_STORE);
	}
}
