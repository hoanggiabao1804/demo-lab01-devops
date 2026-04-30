package com.yas.sampledata.service;

import com.yas.sampledata.utils.SqlScriptExecutor;
import com.yas.sampledata.viewmodel.SampleDataVm;
import javax.sql.DataSource;
import org.springframework.stereotype.Service;

@Service
public class SampleDataService {

    private final SqlScriptExecutor sqlScriptExecutor;
    private final DataSource productDataSource;
    private final DataSource mediaDataSource;

    public SampleDataService(SqlScriptExecutor sqlScriptExecutor,
            DataSource productDataSource,
            DataSource mediaDataSource) {
        this.sqlScriptExecutor = sqlScriptExecutor;
        this.productDataSource = productDataSource;
        this.mediaDataSource = mediaDataSource;
    }

    public SampleDataVm createSampleData() {

        // chạy script cho product DB
        sqlScriptExecutor.executeScriptsForSchema(
                productDataSource,
                "product",
                "sql/sampledata/product");

        // chạy script cho media DB
        sqlScriptExecutor.executeScriptsForSchema(
                mediaDataSource,
                "media",
                "sql/sampledata/media");

        return new SampleDataVm("Insert Sample Data successfully!");
    }
}