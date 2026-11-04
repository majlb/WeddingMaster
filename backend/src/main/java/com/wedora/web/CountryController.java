package com.wedora.web;

import com.wedora.domain.CountryCodes;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class CountryController {
    @GetMapping("/api/countries")
    public List<CountryCodes.CountryOption> list() {
        return CountryCodes.all();
    }
}
