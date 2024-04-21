package com.example.domain.model;


import com.example.config.ModelMapperHolder;
import com.example.domain.model.test002.DestinationRecord;
import com.example.domain.model.test002.DestinationValue;
import com.example.domain.model.test002.SourceRecord;
import com.example.domain.model.test002.SourceValue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static com.example.config.ModelMapperConfig.BUILDER_MODEL_MAPPER;
import static com.example.config.ModelMapperConfig.MODEL_MAPPER;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class Mapping002Test {

    private final ModelMapperHolder modelMapperHolder;

    @Autowired
    public Mapping002Test(ModelMapperHolder modelMapperHolder) {
        this.modelMapperHolder = modelMapperHolder;
    }

    private SourceValue createSource() {

        return SourceValue.builder()
                .firstName("firstName_xxxxx")
                .lastName("lastName_yyyy")
                .fullName("fullName_zzzz")
                .nullField(null)
                .ignoreField("ignore_abc")
                .build();
    }

    private SourceRecord createSourceRecord() {

        return new SourceRecord(
                "firstName",
                "lastName",
                "fullFullName"
        );

    }


    @Test
    @DisplayName("Builderパターンのイミュータブルなクラスへのマッピング")
    void test001() {

        var modelMapper = modelMapperHolder.get(BUILDER_MODEL_MAPPER);

        var source = createSource();
        var actual = modelMapper.map(source, DestinationValue.DestinationValueBuilder.class).build();

        modelMapper
                .getTypeMap(SourceValue.class, DestinationValue.DestinationValueBuilder.class)
                .getMappings().forEach(System.out::println);

        // 結果
        // PropertyMapping[SourceValue.firstName -> DestinationValueBuilder.firstName]
        // PropertyMapping[SourceValue.lastName -> DestinationValueBuilder.lastName]

        assertThat(actual.getFirstName()).isEqualTo(source.getFirstName());
        assertThat(actual.getLastName()).isEqualTo(source.getLastName());
        assertThat(actual.getName()).isNotEqualTo(source.getFullName());

        System.out.println(source);
        System.out.println(actual);

        // 結果
        // SourceValue(firstName=firstName_xxxxx, lastName=lastName_yyyy, nullField=null, fullName=fullName_zzzz, ignoreField=ignore_abc)
        // DestinationValue(firstName=firstName_xxxxx, lastName=lastName_yyyy, name=null, fullFullName=null, fullFillName=null)

    }

    @Test
    @DisplayName("レコードからクラスへのマッピング")
    void test002() {

        var modelMapper = modelMapperHolder.get(BUILDER_MODEL_MAPPER);

        var source = createSourceRecord();
        var actual = modelMapper.map(source, DestinationValue.DestinationValueBuilder.class).build();

        assertThat(actual.getFirstName()).isEqualTo(source.firstName());
        assertThat(actual.getLastName()).isEqualTo(source.lastName());
        assertThat(actual.getFullFullName()).isEqualTo(source.fullFullName());

        System.out.println(source);
        System.out.println(actual);

        // 結果
        // SourceRecord[firstName=firstName, lastName=lastName, fullFullName=fullFullName]
        // DestinationValue(firstName=firstName, lastName=lastName, name=null, fullFullName=fullFullName, fullFillName=null)
    }

    @Test
    @DisplayName("クラスからレコードへのマッピング →　そのままでは対応できず")
    void test003() {

        var modelMapper = modelMapperHolder.get(MODEL_MAPPER);

        var source = createSource();
        var actual = modelMapper.map(source, DestinationRecord.class);

        // 例外
        // org.modelmapper.MappingException: ModelMapper mapping errors:
        //
        // 1) Failed to instantiate instance of destination com.example.domain.model.test002.DestinationRecord. Ensure that com.example.domain.model.test002.DestinationRecord has a non-private no-argument constructor.
        //
        // 1 error
        //
        // 	at org.modelmapper.internal.Errors.throwMappingExceptionIfErrorsExist(Errors.java:386)
        //	at org.modelmapper.internal.MappingEngineImpl.map(MappingEngineImpl.java:80)
        //	at org.modelmapper.ModelMapper.mapInternal(ModelMapper.java:589)
        //	at org.modelmapper.ModelMapper.map(ModelMapper.java:422)
        //	at com.example.domain.model.Mapping002Test.test003(Mapping002Test.java:108)
        //	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
        //	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
        //	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
        //Caused by: java.lang.NoSuchMethodException: com.example.domain.model.test002.DestinationRecord.<init>()
        //	at java.base/java.lang.Class.getConstructor0(Class.java:3761)
        //	at java.base/java.lang.Class.getDeclaredConstructor(Class.java:2930)
        //	at org.modelmapper.internal.MappingEngineImpl.instantiate(MappingEngineImpl.java:336)
        //	at org.modelmapper.internal.MappingEngineImpl.createDestination(MappingEngineImpl.java:351)
        //	at org.modelmapper.internal.MappingEngineImpl.typeMap(MappingEngineImpl.java:140)
        //	at org.modelmapper.internal.MappingEngineImpl.map(MappingEngineImpl.java:114)
        //	at org.modelmapper.internal.MappingEngineImpl.map(MappingEngineImpl.java:71)
        //	... 6 more

    }

    @Test
    @DisplayName("クラスからレコードへのマッピング →　@Builderを付ければ対応可能")
    void test004() {

        var modelMapper = modelMapperHolder.get(BUILDER_MODEL_MAPPER);

        var source = createSource();
        var actual = modelMapper.map(source, DestinationRecord.DestinationRecordBuilder.class).build();

        System.out.println(source);
        System.out.println(actual);

        // 結果
        // SourceValue(firstName=firstName_xxxxx, lastName=lastName_yyyy, nullField=null, fullName=fullName_zzzz, ignoreField=ignore_abc)
        // DestinationRecord[firstName=firstName_xxxxx, lastName=lastName_yyyy, fullFullName=null]
    }

}
