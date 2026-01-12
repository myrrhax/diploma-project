package com.github.myrrhax.diploma_project.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class SchemeService {
    private final SchemaStateCacheStorageService schemaStateCacheStorageService;
}
