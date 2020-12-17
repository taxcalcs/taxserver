package info.kuechler.bmf.test.server;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import info.kuechler.bmf.taxapi.Ausgabe;
import info.kuechler.bmf.taxapi.Eingabe;
import info.kuechler.bmf.taxapi.Lohnsteuer;
import info.kuechler.bmf.taxapi.Type;

@SpringBootTest
public class ControllerTest {

	@Autowired
	private Controller controller;

	@Test
	public void testRush() {
		final Map<String, String> allRequestParams = new HashMap<>();
		allRequestParams.put("LZZ", "1");
		allRequestParams.put("STKL", "1");

		final long start = System.currentTimeMillis();

		for (int i = 0; i < 100000; i++) {
			allRequestParams.put("RE4", Integer.toString(i));
			controller.calc(allRequestParams, i % 16 + 2006).block();
		}
		controller.calc(allRequestParams, 2021).block();
		
		System.out.println("Duration: " + (System.currentTimeMillis() - start) / 1000. + "ms");
	}

	@Test
	public void testConcrete() {
		final Map<String, String> allRequestParams = new HashMap<>();
		allRequestParams.put("LZZ", "1");
		allRequestParams.put("STKL", "1");
		allRequestParams.put("RE4", "12345678");

		final Lohnsteuer lohnsteuer = controller.calc(allRequestParams, 2021).block();
		Assertions.assertEquals("2021", lohnsteuer.getJahr());

		final Eingabe LZZ = findIn(lohnsteuer.getEingaben(), "LZZ");
		Assertions.assertEquals("ok", LZZ.getStatus());
		Assertions.assertEquals(new BigDecimal("1"), LZZ.getValue());

		final Eingabe STKL = findIn(lohnsteuer.getEingaben(), "STKL");
		Assertions.assertEquals("ok", STKL.getStatus());
		Assertions.assertEquals(new BigDecimal("1"), STKL.getValue());

		final Eingabe RE4 = findIn(lohnsteuer.getEingaben(), "RE4");
		Assertions.assertEquals("ok", RE4.getStatus());
		Assertions.assertEquals(new BigDecimal("12345678"), RE4.getValue());
		
		final Ausgabe LSTLZZ = findOut(lohnsteuer.getAusgaben(), "LSTLZZ");
		Assertions.assertEquals(Type.STANDARD, LSTLZZ.getType());
		Assertions.assertEquals(new BigDecimal("3740500"), LSTLZZ.getValue());
	}

	private Eingabe findIn(final Collection<Eingabe> ins, final String findName) {
		for (final Eingabe in : ins) {
			if (findName.equals(in.getName())) {
				return in;
			}
		}
		throw new IllegalStateException();
	}
	
	private Ausgabe findOut(final Collection<Ausgabe> outs, final String findName) {
		for (final Ausgabe out : outs) {
			if (findName.equals(out.getName())) {
				return out;
			}
		}
		throw new IllegalStateException();
	}
}
