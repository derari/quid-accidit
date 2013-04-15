package de.hpi.accidit.orm;

import java.sql.ResultSet;
import java.sql.SQLException;

import de.hpi.accidit.orm.cursor.CursorValue;
import de.hpi.accidit.orm.cursor.ResultCursor;
import de.hpi.accidit.orm.dsl.SelectBuilder;
import de.hpi.accidit.orm.dsl.View;
import de.hpi.accidit.orm.map.Mapping;

public class Person {
	
	String firstName;
	String lastName;
	String address;
	
	public Person() {
	}
	
	public String getFirstName() {
		return firstName;
	}
	public String getLastName() {
		return lastName;
	}
	public String getAddress() {
		return address;
	}
	
	@Override
	public String toString() {
		return firstName + " " + lastName + ", " + address + "\t@" + Integer.toHexString(hashCode());
	}
	
	public static final View<Query> VIEW = new View<Person.Query>() {
		@Override
		public Query newQuery(OConnection cnn, String[] select) {
			return new Query(cnn, select);
		}
	};
	
	protected static class CV extends Person implements CursorValue {
		
		private final ResultCursor<?> rc;
		
		public CV(ResultCursor<?> rc) {
			this.rc = rc;
		}

		@Override
		public ResultCursor<?> getResultCursor() {
			return rc;
		}
	}
	
	protected static final Mapping<Person> MAPPING = new Mapping<Person>(Person.class) {

		@Override
		protected Person newRecord() {
			return new Person();
		}
		
		@Override
		protected Person newCursorValue(final ResultCursor<Person> cursor) {
			return new CV(cursor);
		};

		@Override
		protected void setField(Person p, ResultSet rs, int i, String field) throws SQLException {
			switch (field) {
			case "firstName":
				p.firstName = rs.getString(i);
				break;
			case "lastName":
				p.lastName = rs.getString(i);
				break;
			case "address":
				p.address = rs.getString(i);
				break;
			}
		}
	};
	
	protected static final SelectBuilder FIELDS = new SelectBuilder() {{
		addFields("firstName",	"p.firstName",
				  "lastName",	"p.lastName",
				  "address",	"a.value AS address");
	}};
	
	public static class Query extends de.hpi.accidit.orm.dsl.Query<Person> {
		
		public Query(OConnection cnn, String[] fields) {
			super(cnn, FIELDS, MAPPING);
			select(fields);
			from("Persons p");
			join("Addresses a", "p.addressId = a.id");
		}
		
		public Query where() {
			return this;
		}
		
		public Query lastName_startsWith(String prefix) {
			where("p.lastName LIKE ?");
			addArgument(prefix + "%");
			return this;
		}
		
		@Override
		protected String queryString() {
			String s = super.queryString();
			System.out.println(s);
			return s;
		}
		
	}

}
