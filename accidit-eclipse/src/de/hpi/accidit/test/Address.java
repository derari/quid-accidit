package de.hpi.accidit.test;

import de.hpi.accidit.orm.OConnection;
import java.sql.ResultSet;
import java.sql.SQLException;

import de.hpi.accidit.orm.cursor.CursorValue;
import de.hpi.accidit.orm.cursor.ResultCursor;
import de.hpi.accidit.orm.dsl.QueryByKey;
import de.hpi.accidit.orm.dsl.QueryBuilder;
import de.hpi.accidit.orm.dsl.QueryTemplate;
import de.hpi.accidit.orm.dsl.View;
import de.hpi.accidit.orm.map.Mapping;

public class Address {

	String street;
	String city;
	
	public Address() {
	}

	public String getValue() {
		return street;
	}
	
	@Override
	public String toString() {
		return street + ", " + city + "(@" + Integer.toHexString(hashCode()) + ")";
	}
	
	public static View<Query> VIEW = new View<Query>() {
		@Override
		public Query newQuery(OConnection cnn, String[] select) {
			return new Query(cnn, select);
		}
	};
	
	protected static class CV extends Address implements CursorValue {
		private final ResultCursor<?> rc;
		public CV(ResultCursor<?> rc) {
			this.rc = rc;
		}
		@Override
		public ResultCursor<?> getResultCursor() {
			return rc;
		}
	}
	
	protected static final Mapping<Address> MAPPING = new Mapping<Address>(Address.class) {
		@Override
		protected Address newRecord() {
			return new Address();
		}

		@Override
		protected Address newCursorValue(ResultCursor<Address> cursor) {
			return new CV(cursor);
		}

		@Override
		protected void setField(Address record, String field, ResultSet rs, int i) throws SQLException {
			switch (field) {
			case "street":
				record.street = rs.getString(i);
				break;
			case "city":
				record.city = rs.getString(i);
				break;
			}
		}
	};
	
	protected static final QueryTemplate TEMPLATE = new QueryTemplate() {{
		from("Addresses a");
		select("id", 		"a.id",
				   "street", 	"a.street",
				   "city",		"a.city");
	}};
	
	public static class Query extends QueryBuilder<Address> implements QueryByKey<Address> {

		public Query(OConnection cnn, String[] attributes) {
			super(cnn, TEMPLATE, MAPPING);
			select(attributes);
		}
		
		@Override
		public Query byKeys(final Object... keys) {
			StringBuilder sb = new StringBuilder("a.id IN (");
			for (int i = 0; i < keys.length; i++) {
				if (i > 0) sb.append(", ");
				sb.append('?');
			}
			sb.append(')');
			where(c(sb.toString()), keys);
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
