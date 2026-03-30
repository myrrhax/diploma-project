package com.github.myrrhax.diploma_project.service;

import com.github.myrrhax.diploma_project.repository.ScriptRepository;
import com.github.myrrhax.diploma_project.repository.VersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ScriptGeneratorService {
    private final ScriptRepository scriptRepository;
    private final VersionRepository versionRepository;


}
