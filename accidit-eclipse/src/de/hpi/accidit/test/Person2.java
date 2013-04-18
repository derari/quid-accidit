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

public class Person2 {
	
	String firstName;
	String lastName;
	Address address;
	
	public Person2() {
	}
	
	public String getFirstName() {
		return firstName;
	}
	public String getLastName() {
		return lastName;
	}
	public Address getAddress() {
		return address;
	}
	
	@Override
	public String toString() {
		return firstName + " " + lastName + ", " + address + "\t@" + Integer.toHexString(hashCode());
	}
	
	public static final View<Query> VIEW = new View<Person2.Query>() {
		@Override
		public Query newQuery(OConnection cnn, String[] select) {
			return new Query(cnn, select);
		}
	};
	
	protected static class CV extends Person2 implements CursorValue {
		
		private final ResultCursor<?> rc;
		
		public CV(ResultCursor<?> rc) {
			this.rc = rc;
		}

		@Override
		public ResultCursor<?> getResultCursor() {
			return rc;
		}
	}
	
	protected static final Mapping<Person2> MAPPING = new Mapping<Person2>(Person2.class) {

		@Override
		protected Person2 newRecord() {
			return new Person2();
		}
		
		@Override
		protected Person2 newCursorValue(final ResultCursor<Person2> cursor) {
			return new CV(cursor);
		};

		@Override
		protected void setField(Person2 record, String field, ResultSet rs, int i) throws SQLException {
			switch (field) {
			case "firstName":
				record.firstName = rs.getString(i);
				break;
			case "lastName":
				record.lastName = rs.getString(i);
				break;
			default:
				super.setField(record, field, rs, i);
			}
		}
		
		public void setField(Person2 record, String field, Object value) throws SQLException {
			switch (field) {
			case "address":
				record.address = (Address) value;
				break;
			default:
				super.setField(record, field, value);
			}
		};
	};
	
	protected static QueryTemplate TEMPLATE = new QueryTemplate() {{
		
		from("Persons p");
		select("firstName", "p.firstName",
				   "lastName",	"p.lastName");
		internal_select("addressId",	"p.addressId");
		where("firstName_LIKE", 	"firstName LIKE ?",
				   "lastName_LIKE", 	"lastName LIKE ?");
		
		optinal_join("a", "Addresses a").on("p.addressId = a.id");
		using("a")
			.where("address_LIKE", 	"a.value LIKE ?");
		
		using("addressId")
			.relation("address", Address.VIEW);
		
	}};
	
	public static class Query extends QueryBuilder<Person2> {
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
