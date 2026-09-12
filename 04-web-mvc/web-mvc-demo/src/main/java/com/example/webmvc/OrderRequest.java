package com.example.webmvc;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record OrderRequest(@NotBlank String item, @Min(1) int qty) {}
