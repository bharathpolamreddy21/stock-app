package com.stockroom.controller;

import com.stockroom.config.AppProperties;
import com.stockroom.service.ExchangeService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/exchange")
public class ExchangeController {

    private final ExchangeService exchangeService;
    private final AppProperties   props;

    public ExchangeController(ExchangeService exchangeService, AppProperties props) {
        this.exchangeService = exchangeService;
        this.props           = props;
    }

    /**
     * GET /api/exchange?base=USD
     * Returns live rates (if EXCHANGE_API_KEY set) or mock rates.
     * Results are cached for EXCHANGE_CACHE_TTL_SECONDS.
     */
    @GetMapping
    public Map<String, Object> getRates(
            @RequestParam(defaultValue = "") String base) {

        String effectiveBase = (base == null || base.isBlank())
                ? props.getExchange().getDefaultBase()
                : base.toUpperCase();

        return exchangeService.getRates(effectiveBase);
    }
}
