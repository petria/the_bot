package org.freakz.engine.commands.util;

import org.freakz.engine.services.weather.weatherapi.model.Astro;
import org.freakz.engine.services.weather.weatherapi.model.AstronomyResponse;
import org.freakz.engine.services.weather.weatherapi.model.ForecastResponse;

public class WeatherUtils {

  public static String formatTime(ForecastResponse r) {
    return String.format("%s", r.current().last_updated().split(" ")[1]);
  }

  public static String formatAstronomy(AstronomyResponse r, boolean doAstronomy) {
    if (doAstronomy && r != null) {
      Astro a = r.astronomy().astro();
      return String.format(
          " - sun up: %s / down %s -- moon: %s", a.sunrise(), a.sunset(), a.moon_phase());
    }
    return "";
  }

  public static String formatFeelsLike(ForecastResponse r, boolean doFeelsLike) {
    if (doFeelsLike) {
      return String.format(" (feels like %s°C)", r.current().feelslike_c());
    }
    return "";
  }

  public static String formatName(ForecastResponse r, boolean verbose) {
    if (verbose) {
      return String.format(
          "%s/%s/%s", r.location().name(), r.location().region(), r.location().country());
    }
    return String.format("%s", r.location().name());
  }

}
