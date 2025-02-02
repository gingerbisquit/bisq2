/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.common.currency;

import bisq.common.locale.CountryRepository;
import bisq.common.locale.LocaleRepository;
import lombok.Getter;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FiatCurrencyRepository {
    private static Map<String, FiatCurrency> currencyByCode;
    @Getter
    private static List<FiatCurrency> allCurrencies;
    @Getter
    private static List<FiatCurrency> majorCurrencies;
    @Getter
    private static List<FiatCurrency> minorCurrencies;
    @Getter
    private static FiatCurrency defaultCurrency;

    static {
        initialize(LocaleRepository.getDefaultLocale());
    }

    // Need to be called at application setup with user locale
    public static void initialize(Locale locale) {
        currencyByCode = CountryRepository.getCountries().stream()
                .map(country -> getCurrencyByCountryCode(country.code(), locale))
                .distinct()
                .collect(Collectors.toMap(FiatCurrency::getCode, Function.identity(), (x, y) -> x, HashMap::new));

        defaultCurrency = getCurrencyByCountryCode(locale.getCountry(), locale);
        majorCurrencies = initMajorCurrencies(LocaleRepository.getDefaultLocale());
        minorCurrencies = new ArrayList<>(currencyByCode.values());
        minorCurrencies.remove(defaultCurrency);
        minorCurrencies.removeAll(majorCurrencies);
        minorCurrencies.sort(Comparator.comparing(TradeCurrency::getNameAndCode));
        allCurrencies = new ArrayList<>();
        allCurrencies.add(defaultCurrency);
        allCurrencies.addAll(majorCurrencies);
        allCurrencies.addAll(minorCurrencies);
    }

    private static List<FiatCurrency> initMajorCurrencies(Locale locale) {
        List<String> mainCodes = new ArrayList<>(List.of("USD", "EUR", "GBP", "CAD", "AUD", "RUB", "INR", "NGN"));
        mainCodes.add(0, defaultCurrency.code);
        return mainCodes.stream()
                .map(code -> currencyByCode.get(code))
                .distinct()
                .collect(Collectors.toList());
    }

    public static FiatCurrency getCurrencyByCountryCode(String countryCode) {
        return getCurrencyByCountryCode(countryCode, LocaleRepository.getDefaultLocale());
    }

    public static FiatCurrency getCurrencyByCountryCode(String countryCode, Locale locale) {
        if (countryCode.equals("XK")) {
            return new FiatCurrency("EUR", locale);
        }

        Currency currency = Currency.getInstance(new Locale(locale.getLanguage(), countryCode));
        return new FiatCurrency(currency.getCurrencyCode(), locale);
    }

    public static Map<String, FiatCurrency> getCurrencyByCodeMap() {
        return currencyByCode;
    }

    public static FiatCurrency getCurrencyByCode(String code) {
        return currencyByCode.get(code);
    }
}
