package com.example.domain.model;

import com.example.domain.model.test001.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.modelmapper.AbstractConverter;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
class Mapping001Test {

    @Test
    @DisplayName("標準設定でModelMapperを使う、マッピング結果を出力")
    void test001() {

        var mapper = new ModelMapper();

        var source = new Source();
        source.setFirstName("firstName_xxx");
        source.setLastName("lastName_xxx");

        var target = mapper.map(source, Target.class);
        mapper.getTypeMap(Source.class, Target.class).getMappings().forEach(System.out::println);

        // 結果
        // PropertyMapping[Source.companyAddress -> Target.address]
        // PropertyMapping[Source.companyAddress -> Target.company]
        // PropertyMapping[Source.firstName -> Target.firstName]

        assertThat(target.getFirstName()).isEqualTo(source.getFirstName());
        assertThat(target.getFullName()).isNull();
    }

    @Test
    @DisplayName("標準設定で、マッチングが失敗するケース")
    void test002() {

        var mapper = new ModelMapper();

        var source = new Source2();
        source.setFirstName("firstName_xxx");
        source.setLastName("lastName_xxx");

        var target = mapper.map(source, Target2.class);

        //　結果(例外発生)
        // org.modelmapper.ConfigurationException: ModelMapper configuration errors:

        // 1) The destination property com.example.domain.model.test001.Target2.setAddress() matches multiple source property hierarchies:

        // com.example.domain.model.test001.Source2.getCompanyAddress()
        // com.example.domain.model.test001.Source2.getHomeAddress()

        // 1 error

        // at org.modelmapper.internal.Errors.throwConfigurationExceptionIfErrorsExist(Errors.java:240)
        // at org.modelmapper.internal.ImplicitMappingBuilder.matchDestination(ImplicitMappingBuilder.java:159)
        // at org.modelmapper.internal.ImplicitMappingBuilder.build(ImplicitMappingBuilder.java:90)
        // at org.modelmapper.internal.ImplicitMappingBuilder.build(ImplicitMappingBuilder.java:75)
        // at org.modelmapper.internal.TypeMapStore.getOrCreate(TypeMapStore.java:131)
        // at org.modelmapper.internal.TypeMapStore.getOrCreate(TypeMapStore.java:106)
        // at org.modelmapper.internal.MappingEngineImpl.map(MappingEngineImpl.java:112)
        // at org.modelmapper.internal.MappingEngineImpl.map(MappingEngineImpl.java:71)
        // at org.modelmapper.ModelMapper.mapInternal(ModelMapper.java:589)
        // at org.modelmapper.ModelMapper.map(ModelMapper.java:422)
        // at com.example.domain.model.Mapping001Test.test002(Mapping001Test.java:51)
        // at java.base/java.lang.reflect.Method.invoke(Method.java:580)
        // at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
        // at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
    }

    @Test
    @DisplayName("厳密なルールを適用")
    void test003() {

        var mapper = new ModelMapper();
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);

        var source = new Source();
        source.setFirstName("firstName_xxx");
        source.setLastName("lastName_xxx");

        var target = mapper.map(source, Target.class);
        mapper.getTypeMap(Source.class, Target.class).getMappings().forEach(System.out::println);

        // 結果
        // PropertyMapping[Source.firstName -> Target.firstName]

        assertThat(target.getFirstName()).isEqualTo(source.getFirstName());
        assertThat(target.getFullName()).isNull();
    }


    @Test
    @DisplayName("厳密なルールを適用することで、例外が回避される")
    void test004() {

        var mapper = new ModelMapper();
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);

        var source = new Source2();
        source.setFirstName("firstName_xxx");
        source.setLastName("lastName_xxx");

        var target = mapper.map(source, Target2.class);
        mapper.getTypeMap(Source2.class, Target2.class).getMappings().forEach(System.out::println);

        // 結果
        // PropertyMapping[Source2.companyAddress -> Target2.companyAddress]
        // PropertyMapping[Source2.firstName -> Target2.firstName]
        // PropertyMapping[Source2.homeAddress -> Target2.homeAddress]

        assertThat(target.getFirstName()).isEqualTo(source.getFirstName());
        assertThat(target.getFullName()).isNull();
    }

    @Test
    @DisplayName("自動でマッチングしない組み合わせを追加")
    void test005() {

        var mapper = new ModelMapper();
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);

        // 組み合わせを追加
        mapper.typeMap(Source.class, Target.class)
                .addMapping(Source::getCompanyAddress, Target::setFullName);

        var source = new Source();
        source.setFirstName("firstName_xxx");
        source.setLastName("lastName_xxx");

        var target = mapper.map(source, Target.class);
        mapper.getTypeMap(Source.class, Target.class).getMappings().forEach(System.out::println);

        // 結果
        // PropertyMapping[Source.firstName -> Target.firstName]
        // PropertyMapping[Source.companyAddress -> Target.fullName]

        assertThat(target.getFirstName()).isEqualTo(source.getFirstName());
        assertThat(target.getFullName()).isNull();
    }


    @Test
    @DisplayName("createTypeMapを使ってマッチングを検証(標準設定)")
    void test006() {
        var mapper = new ModelMapper();
        mapper.createTypeMap(Source.class, Target.class).getMappings().forEach(System.out::println);

        // 結果
        // PropertyMapping[Source.companyAddress -> Target.address]
        // PropertyMapping[Source.companyAddress -> Target.company]
        // PropertyMapping[Source.firstName -> Target.firstName]
    }

    @Test
    @DisplayName("createTypeMapを使ってマッチングを検証(厳密ルール設定)")
    void test007() {
        var mapper = new ModelMapper();
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT); // 厳密ルールに設定
        mapper.createTypeMap(Source.class, Target.class).getMappings().forEach(System.out::println);

        // 結果
        // PropertyMapping[Source.firstName -> Target.firstName]
    }


    @Test
    @DisplayName("マッピングをスキップ")
    void test008() {
        var mapper = new ModelMapper();
        mapper.createTypeMap(Source.class, Target.class)
                .addMappings(x -> x.skip(Target::setCompany))
                .getMappings().forEach(System.out::println);

        // 結果
        // PropertyMapping[Source.companyAddress -> Target.address]
        // ConstantMapping[null -> Target.company]
        // PropertyMapping[Source.firstName -> Target.firstName]
    }


    @Test
    @DisplayName("オブジェクト型のフィールドのマッピングを検証")
    void test009() {
        var mapper = new ModelMapper();
        mapper.createTypeMap(Source3.class, Target3.class)
                .getMappings().forEach(System.out::println);

        // 結果
        // PropertyMapping[Source3.person -> Target3.person]

        var source = new Source3();
        var target = mapper.map(source, Target3.class);

        assertThat(target.getPerson()).isEqualTo(source.getPerson());

    }

    @Test
    @DisplayName("DeepCopyを有効にしたオブジェクト型フィールドのマッピング")
    void test010() {
        var mapper = new ModelMapper();
        mapper.getConfiguration().setDeepCopyEnabled(true);
        mapper.createTypeMap(Source3.class, Target3.class)
                .getMappings().forEach(System.out::println);

        // 結果
        // PropertyMapping[Source3.person.firstName -> Target3.person.firstName]
        // PropertyMapping[Source3.person.lastName -> Target3.person.lastName]

        var source = new Source3();
        source.setPerson(new Person("abc", "efg"));
        var target = mapper.map(source, Target3.class);
        source.getPerson().setFirstName("firstName_changed");
        System.out.println(source);
        System.out.println(target);

        // 結果
        // Source3(person=Person(firstName=firstName_changed, lastName=efg))
        // Target3(person=Person(firstName=abc, lastName=efg))
    }

    @Test
    @DisplayName("送信元がnullの場合、更新をスキップする設定")
    void test011_1() {
        var mapper = new ModelMapper();

        var source = new Source();
        source.setFirstName(null); // 送信元はnull
        var target = new Target();
        target.setFirstName("target_firstName"); // 送信先は値あり

        mapper.map(source, target);

        System.out.println("source: " + source.getFirstName());
        System.out.println("target: " + target.getFirstName());

        // 結果
        // source: null
        // target: null // 上書きされた
    }


    @Test
    @DisplayName("送信元がnullの場合、更新をスキップする設定")
    void test011_2() {
        var mapper = new ModelMapper();
        mapper.getConfiguration().setSkipNullEnabled(true);

        var source = new Source();
        source.setFirstName(null); // 送信元はnull
        var target = new Target();
        target.setFirstName("target_firstName"); // 送信先は値あり

        mapper.map(source, target);

        System.out.println("source: " + source.getFirstName());
        System.out.println("target: " + target.getFirstName());

        // 結果
        // source: null
        // target: target_firstName // 上書きされなかった
    }


    @Test
    @DisplayName("リスト型のマッピング(標準設定)")
    void test012_1() {
        var mapper = new ModelMapper();

        var source = new Source4();
        source.setStringList(List.of("a", "b", "c"));
        var target = new Target4();
        target.setStringList(List.of("e", "f", "g", "h"));

        mapper.map(source, target);

        System.out.println("source: " + source.getStringList());
        System.out.println("target: " + target.getStringList());

        // 結果
        // source: [a, b, c]
        // target: [a, b, c, h]  // 送信先の方が件数が多い場合、更新されない。

    }

    @Test
    @DisplayName("リスト型のフィールドを洗い替えする様に変更")
    void test012_2() {
        var mapper = new ModelMapper();
        mapper.getConfiguration().setCollectionsMergeEnabled(false);

        var source = new Source4();
        source.setStringList(List.of("a", "b", "c"));
        var target = new Target4();
        target.setStringList(List.of("e", "f", "g", "h"));

        mapper.map(source, target);

        System.out.println("source: " + source.getStringList());
        System.out.println("target: " + target.getStringList());

        // 結果
        // source: [a, b, c]
        // target: [a, b, c]
    }


    @Test
    @DisplayName("コンバーターにより異なる型間のマッピングをカスタマイズする")
    void test013() {
        var mapper = new ModelMapper();
        mapper.addConverter(listToString);

        var source = new Source4();
        source.setStringList(List.of("a", "b", "c"));

        var target = mapper.map(source, Target4_1.class);

        System.out.println("source: " + source.getStringList());
        System.out.println("target: " + target.getStringList());

        // 結果
        // source: [a, b, c]
        // target: a,b,c
    }

    /**
     * コンバーター(List<String>-> String)
     */
    Converter<List<String>, String> listToString = new AbstractConverter<>() {
        @Override
        protected String convert(List<String> source) {
            return String.join(",", source);
        }
    };
}
