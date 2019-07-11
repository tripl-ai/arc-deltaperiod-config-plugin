arc-deltaperiod-config-plugin creates a list of formatted dates to easily calculate data processing periods.

## Documentation

To use the plugin specify a return format, the lag and lead days and a variable name to return. This example will create a string like `2019-07-01,2019-07-02,2019-07-03,2019-07-04,2019-07-05,2019-07-06,2019-07-07,2019-07-08,2019-07-09,2019-07-10,2019-07-11,2019-07-12`. Optionally `currentDate` can be specified in the same format as the `formatter` to specify a deterministic date for testing.

This string can then be used with the `glob` processing capabilities of Spark to read a subset of input files:

```json
{
  "plugins": {
    "config": [
      {
        "type": "DeltaPeriodDynamicConfigurationPlugin",
        "environments": [
          "production",
          "test"
        ],
        "returnName": "ETL_CONF_DELTA_PERIOD",
        "lagDays": 10,
        "leadDays": 1,
        "formatter": "uuuu-MM-dd"
      }      
    ]
  },
  "stages": [
    {
      "type": "ParquetExtract",
      "name": "test",
      "description": "test",
      "environments": [
        "production",
        "test"
      ],
      "inputURI": "/tmp/customer/{"${ETL_CONF_DELTA_PERIOD}"}/*.parquet",
      "outputView": "test"
    }
  ]
}
```

## Authors/Contributors

- [Mike Seddon](https://github.com/seddonm1)


## License

Arc is released under the [MIT License](https://opensource.org/licenses/MIT).
