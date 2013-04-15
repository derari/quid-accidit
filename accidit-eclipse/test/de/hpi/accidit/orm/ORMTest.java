package de.hpi.accidit.orm;

import static de.hpi.accidit.orm.dsl.Select.select;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.concurrent.Future;

import de.hpi.accidit.orm.cursor.OFutureCursor;
import de.hpi.accidit.orm.cursor.ResultCursor;
import de.hpi.accidit.orm.dsl.View;

public class ORMTest {

	static final View<Person.Query> PERSONS = Person.VIEW;
	
	public static void main(String[] args) throws Exception {
		String dbString = "jdbc:mysql://localhost:3306/ormtest?user=root&password=root";
		
		Connection jdbc = DriverManager.getConnection(dbString);
		OConnection cnn = new OConnection(jdbc);
		
		Person.Query qryPersons_D = select().from(PERSONS).where().lastName_startsWith("D");
		
		Future<Person[]> f = qryPersons_D.submit(cnn).asArray();
		Person[] persons = f.get();
		for (Person p: persons) {
			System.out.println(p);
		}
		
		System.out.println();
		
		Future<ResultCursor<Person>> fc = qryPersons_D.submit(cnn).asCursor();
		for (Person p: fc.get()) {
			System.out.println(p);
		}
		
		System.out.println();
		
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
		
		cnn.close();
	}
	
	/*
	 
	 qryPersons_D.submit(cnn);
	 
	 MIStatement getMethods = cnn.select().from(INVOCATION);
	 XFuture<MethodInvocation[]> getMethods.atStep(1, 0).withIds(1, 2, 3).run();
	 
	 
	XFuture<MethodInvocation[]> = cnn.select("enter, exit")
			.from(INVOCATION)
			.atStep(1, 0)
			.forIds(1, 2, 3);


	 */
	
}
