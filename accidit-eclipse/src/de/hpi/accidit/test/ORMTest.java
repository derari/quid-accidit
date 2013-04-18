package de.hpi.accidit.test;

import static de.hpi.accidit.orm.dsl.Select.select;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.concurrent.Future;

import de.hpi.accidit.orm.OConnection;
import de.hpi.accidit.orm.OFuture;
import de.hpi.accidit.orm.OFutureAction;
import de.hpi.accidit.orm.cursor.OFutureCursor;
import de.hpi.accidit.orm.cursor.ResultCursor;
import de.hpi.accidit.orm.dsl.View;

public class ORMTest {

	static final View<Person.Query> PERSONS = Person.VIEW;
	static final View<Person2.Query> PERSONS2 = Person2.VIEW;
	
	public static void main(String[] args) throws Exception {
		String dbString = "jdbc:mysql://localhost:3306/ormtest?user=root&password=root";
		
		Connection jdbc = DriverManager.getConnection(dbString);
		OConnection cnn = new OConnection(jdbc);
		
		test_arrayResult(cnn);
		test_cursorResult(cnn);
		test_asyncProcessing(cnn);
		test_autoJoin_notSelected(cnn);
		test_relation(cnn);
		test_cursor_relation(cnn);
		test_relation_select1(cnn);
		
		cnn.close();
	}
	
	public static void test_arrayResult(OConnection cnn) throws Exception {
		Person.Query qryPersons_D = select().from(PERSONS).where().lastName_startsWith("D");
		
		Future<Person[]> f = qryPersons_D.submit(cnn).asArray();
		Person[] persons = f.get();
		for (Person p: persons) {
			System.out.println(p);
		}
		
		System.out.println();
	}
	
	public static void test_cursorResult(OConnection cnn) throws Exception {
		Person.Query qryPersons_D = select().from(PERSONS).where().lastName_startsWith("D");
		
		Future<ResultCursor<Person>> fc = qryPersons_D.submit(cnn).asCursor();
		for (Person p: fc.get()) {
			System.out.println(p);
		}
		
		System.out.println();
	}
	
	public static void test_asyncProcessing(OConnection cnn) throws Exception {
		Person.Query qryPersons_D = select().from(PERSONS).where().lastName_startsWith("D");
		
		OFutureCursor<Person> fc2 = qryPersons_D.submit(cnn).asCursor();
		OFuture<String> text = fc2.onCompleteC(new OFutureAction<OFutureCursor<Person>, String>() {
			@Override
			public String call(OFutureCursor<Person> fPersons) throws Exception {
				StringBuilder sb = new StringBuilder();
				for (Person p: fPersons.get()) {
					sb.append(p.toString()).append("\n");
				}
				return sb.toString();
			}
		});
		System.out.println(text.get());
		
		System.out.println();
	}
	
	public static void test_autoJoin_notSelected(OConnection cnn) throws Exception {
		OFuture<Person[]> f3 = cnn.select("firstName")
				  .from(PERSONS)
				  .where().address_like("%street%")
				  .submit().asArray();
		
		for (Person p: f3.get()) {
			System.out.println(p);
		}
		
		System.out.println();
	}
	
	public static void test_relation(OConnection cnn) throws Exception {
		Person2[] pp = cnn.select().from(PERSONS2).run().asArray();
		
		for (Person2 p: pp) {
			System.out.println(p);
		}
		
		System.out.println();
	}
	
	public static void test_cursor_relation(OConnection cnn) throws Exception {
		ResultCursor<Person2> pp = cnn.select().from(PERSONS2).run().asCursor();
		
		for (Person2 p: pp) {
			System.out.println(p);
		}
		
		System.out.println();
	}
	
	public static void test_relation_select1(OConnection cnn) throws Exception {
		Person2[] pp = cnn.select("lastName", "address.city").from(PERSONS2).run().asArray();
		
		for (Person2 p: pp) {
			System.out.println(p);
		}
		
		System.out.println();
	}

	
}
