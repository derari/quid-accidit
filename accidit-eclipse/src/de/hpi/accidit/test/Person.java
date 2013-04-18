package de.hpi.accidit.test;

import de.hpi.accidit.orm.OConnection;
import java.sql.ResultSet;
import java.sql.SQLException;

import de.hpi.accidit.orm.cursor.CursorValue;
import de.hpi.accidit.orm.cursor.ResultCursor;
import de.hpi.accidit.orm.dsl.QueryBuilder;
import de.hpi.accidit.orm.dsl.QueryTemplate;
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
		protected void setField(Person record, String field, ResultSet rs, int i) throws SQLException {
			switch (field) {
			case "firstName":
				record.firstName = rs.getString(i);
				break;
			case "lastName":
				record.lastName = rs.getString(i);
				break;
			case "address":
				record.address = rs.getString(i);
				break;
			}
		}
	};
	
	protected static QueryTemplate TEMPLATE = new QueryTemplate() {{
		
		from("Persons p");
		select("firstName", "p.firstName",
				   "lastName",	"p.lastName");
		where("firstName_LIKE", 	"firstName LIKE ?",
				   "lastName_LIKE", 	"lastName LIKE ?");
		
		optinal_join("a", "Addresses a").on("p.addressId = a.id");
		using("a")
			.select("address", "a.street AS address")
			.where("address_LIKE", "a.street LIKE ?");
		
	}};
	
	public static class Query extends QueryBuilder<Person> {
		public Query(OConnection cnn, String[] attributes) {
			super(cnn, TEMPLATE, MAPPING);
			select(attributes);
		}
		
		public Query where() {
			return this;
		}
		
		public Query lastName_startsWith(String prefix) {
			where("lastName_LIKE", prefix + "%");
			return this;
		}
		
		public Query address_like(String pattern) {
			where("address_LIKE", pattern);
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
