package com.example.bookexchange.common.demoreset;

public interface DemoSeedImporter {

    void validateSeedAvailable(String seedS3Location);

    void importSeed(String seedS3Location);
}
