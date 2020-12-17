package info.kuechler.bmf.test.server;

import static java.util.stream.Collectors.toList;

import java.math.BigDecimal;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import info.kuechler.bmf.taxapi.Ausgabe;
import info.kuechler.bmf.taxapi.Eingabe;
import info.kuechler.bmf.taxapi.Lohnsteuer;
import info.kuechler.bmf.taxapi.ObjectFactory;
import info.kuechler.bmf.taxapi.Type;
import info.kuechler.bmf.taxcalculator.Accessor;
import info.kuechler.bmf.taxcalculator.rw.ReadWriteException;
import info.kuechler.bmf.taxcalculator.rw.Reader;
import info.kuechler.bmf.taxcalculator.rw.TaxCalculatorFactory;
import info.kuechler.bmf.taxcalculator.rw.Writer;
import reactor.core.publisher.Mono;

@RestController
public class Controller {

	private static final String STATUS_NOK = "nok";
	private static final String STATUS_OK = "ok";

	private final ObjectFactory objectFactory = new ObjectFactory();
	private final TaxCalculatorFactory taxCalculatorFactory = new TaxCalculatorFactory();

	private final ConcurrentMap<String, Map<String, Class<?>>> CACHE_INPUT_TYPES = new ConcurrentHashMap<>();
	private final ConcurrentMap<String, Map<String, Class<?>>> CACHE_OUTPUT_TYPES = new ConcurrentHashMap<>();

	@GetMapping("/{year}")
	public Mono<Lohnsteuer> calc(@RequestParam final Map<String, String> allRequestParams,
			@PathVariable final int year) {
		return calc(allRequestParams, year, 0);
	}

	@GetMapping("/{year}/{month}")
	public Mono<Lohnsteuer> calc(@RequestParam final Map<String, String> allRequestParams, @PathVariable final int year,
			@PathVariable final int month) {
		return Mono.fromSupplier(() -> getTax(year, month, allRequestParams));
	}

	private Lohnsteuer getTax(final int year, final int month, final Map<String, String> inputs) {
		final String classKey = getTaxCalculatorFactory().getYearKey(month, year);

		// TODO do not use accessor
		final Accessor<String, ?> accessor = createAccessor(year, month);
		final Map<String, Class<?>> inputClasses = getInputTypes(classKey);

		final Lohnsteuer lohnsteuer = getObjectFactory().createLohnsteuer();

		// year
		lohnsteuer.setJahr(Integer.toString(year));

		// information
		lohnsteuer.setInformation("Created by " + getClass().getPackage().getName());

		// create inputs elements for repeating in output
		final List<Eingabe> inputElements = inputs.entrySet().stream().map(createInputElement(inputClasses))
				.collect(toList());
		lohnsteuer.setEingaben(inputElements);

		// only calculate if input elements are valid
		if (!inputElements.stream().filter(isValid().negate()).findAny().isPresent()) {
			final Writer writer = createWriter(classKey);

			// convert inputs in correct type and set into writer
			inputs.entrySet().stream()
					.map(e -> new SimpleImmutableEntry<String, Object>(e.getKey(),
							convertToType(e.getValue(), inputClasses.get(e.getKey()))))
					.forEach(setInputsInWriter(writer));

			final Reader reader = createReader(writer);

			// get output names and read it from reader
			lohnsteuer.setAusgaben(getOutputTypes(classKey).entrySet().stream()
					.map(createOutputElement(reader, accessor)).collect(toList()));
		}
		return lohnsteuer;
	}

	private Accessor<String, ?> createAccessor(final int year, final int month) {
		return wrap(() -> TaxCalculatorFactory.createWithAccessor(month, year));
	}

	private Consumer<Entry<String, ?>> setInputsInWriter(final Writer writer) {
		return inputValue -> wrap(() -> writer.set(inputValue.getKey(), inputValue.getValue()));
	}

	private Function<Entry<String, Class<?>>, Ausgabe> createOutputElement(final Reader reader,
			final Accessor<String, ?> accessor) {
		return entry -> wrap(() -> {
			final String outputName = entry.getKey();
			final Ausgabe output = getObjectFactory().createAusgabe();
			output.setName(outputName);
			output.setType(Type.fromValue(accessor.getOutputSpecialType(outputName)));
			output.setValue(readValue(reader, outputName, entry.getValue()));
			return output;
		});
	}

	private Function<Entry<String, String>, Eingabe> createInputElement(final Map<String, Class<?>> inputClasses) {
		return inputParameter -> {
			final Eingabe eingabe = getObjectFactory().createEingabe();
			eingabe.setName(inputParameter.getKey());
			if (inputClasses.containsKey(inputParameter.getKey())) {
				try {
					eingabe.setValue(new BigDecimal(inputParameter.getValue()));
					eingabe.setStatus(STATUS_OK);
				} catch (NumberFormatException e) {
					eingabe.setStatus(STATUS_NOK);
				}
			} else {
				eingabe.setStatus(STATUS_NOK);
			}
			return eingabe;
		};
	}

	private Predicate<Eingabe> isValid() {
		return e -> STATUS_OK.equals(e.getStatus());
	}

	private Map<String, Class<?>> getInputTypes(final String classKey) {
		return CACHE_INPUT_TYPES.computeIfAbsent(classKey,
				ck -> wrap(() -> getTaxCalculatorFactory().getInputsWithType(ck)));
	}

	private Map<String, Class<?>> getOutputTypes(final String classKey) {
		return CACHE_OUTPUT_TYPES.computeIfAbsent(classKey,
				ck -> wrap(() -> getTaxCalculatorFactory().getOutputsWithType(ck)));
	}

	private BigDecimal readValue(final Reader reader, final String name, final Class<?> type)
			throws ReadWriteException {
		if (type == BigDecimal.class) {
			return reader.getBigDecimal(name);
		}
		if (type == int.class) {
			return new BigDecimal(reader.getInt(name));
		}
		if (type == double.class) {
			return new BigDecimal(reader.getDouble(name));
		}
		throw new IllegalStateException("Type unknown " + name);
	}

	private Object convertToType(final String value, final Class<?> type) {
		if (value == null || "".equals(value)) {
			return null;
		}
		if (type == BigDecimal.class) {
			return new BigDecimal(value);
		}
		if (type == int.class) {
			return Integer.parseInt(value);
		}
		if (type == double.class) {
			return Double.parseDouble(value);
		}
		return value;
	}

	private Writer createWriter(final String classKey) {
		return wrap(() -> getTaxCalculatorFactory().create(classKey).setAllToZero());
	}

	private Reader createReader(final Writer writer) {
		return wrap(() -> writer.calculate());
	}

	private ObjectFactory getObjectFactory() {
		return objectFactory;
	}

	private TaxCalculatorFactory getTaxCalculatorFactory() {
		return taxCalculatorFactory;
	}

	@FunctionalInterface
	private interface SupplierEx<T> {
		T get() throws Exception;
	}

	private <T> T wrap(final SupplierEx<T> supplier) {
		try {
			return supplier.get();
		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
	}
}
