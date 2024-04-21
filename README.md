# ModelMapper 利用方法(検討中)

バージョンアップが止まってしまったDozerの代替方法として、ModelMapperを評価する。

構造の大きく異なるModel間のマッピングの要件はそれほど多くない前提で、安全な標準設定を行い、
ルールに逸脱したマッピング要件に対してどの様にカスタマイズすれば良いかについての知見を収集する。

## デフォルト設定が危険な理由

### 意図しないマッチング
ModelMapperは完全一致していないフィールド同士を自動的にマッピングする機能が標準で動作する。送信元・送信先モデルのフィールド名を単語に分割して、
単語単位にマッチするフィールドを選定するため、少々異なるフィールドも自動的にマッチングしてしまう。

#### 標準のマッチングルール
- トークンは任意の順序で照合できます
- すべての送信元フィールド名のトークンが一致する必要があります
- すべての送信先フィールド名には少なくとも1つのトークンが一致する必要があります

以下のモデルをBeanマッピングするとします。
```java
@Data
public class Source {
    private String firstName;
    private String lastName;
    private String companyAddress;
}

@Data
public class Target {
    private String firstName;
    private String fullName;
    private String address;
    private String company;
}
```
標準設定でマッピングした場合の、単語の一部が一致するマッピングが行われます。
```java
var mapper = new ModelMapper();
var source = new Source();
source.setFirstName("firstName_xxx");
source.setLastName("lastName_xxx");
var target = mapper.map(source, Target.class);

// 結果
// Source.firstName -> Target.firstName
// Source.companyAddress -> Target.address // addressが一致
// Source.companyAddress -> Target.compnay // companyが一致
```

また、以下の様にうまくマッチングできない（一意にフィールドが特定できない）ケースが発生すると、例外が発生します。
```java
@Data
public class Source2 {
  private String homeAddress;
  private String companyAddress;
}
@Data
public class Target2 {
    private String address; // homeAddress, companyAddressの両方にマッチする
}

// 結果
// org.modelmapper.ConfigurationException: ModelMapper configuration errors:
// 1) The destination property com.example.domain.model.test001.Target2.setAddress() matches multiple source property hierarchies:
//  com.example.domain.model.test001.Source2.getHomeAddress()
//  com.example.domain.model.test001.Source2.getCompanyAddress()
```
#### 厳密なマッチングルール
厳密なマッピングルールを適用すると完全に一致したフィールドのみマッチングされる様にカスタマイズできます。

```java
var mapper = new ModelMapper();
mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);

// 結果
// Source.firstName -> Target.firstName
```

厳密なマッチングルールを利用すると、マッチングが特定できない例外も発生しません。

### マッピング結果の確認方法
以下のコードで、実際にマッピングされたフィールドの組み合わせを確認できます。createTypeMap()は複数回実行できません。
```java
var mapper = new ModelMapper();
mapper.createTypeMap(Source.class, Target.class).getMappings().forEach(System.out::println);

// 結果
// PropertyMapping[Source.companyAddress -> Target.address]
// PropertyMapping[Source.companyAddress -> Target.company]
// PropertyMapping[Source.firstName -> Target.firstName]
```

厳格ルールで結果を確認
```java
var mapper = new ModelMapper();
mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT); // 厳密ルールに設定
mapper.createTypeMap(Source.class, Target.class).getMappings().forEach(System.out::println);

// 結果
// PropertyMapping[Source.firstName -> Target.firstName]
```

### Bean登録したModelMapperの変更はシステム全体に及ぶ
ModelMapperをBean登録すると、システム全体でオブジェクトを共有することになり、設定がどこからでも変更できる状態になっており、設定変更はシステム全体に反映する。
個々のプログラム内で設定を変更することは非常に危険である。設定を変更したい場合は、設定毎に名前を変更してBean登録することが推奨される。



## カスタマイズ機能

ModelMapperが提供するカスタマイズ機能を調査していきます。

- 命名規則に合わないフィールドの組み合わせを指定する
- 一部のフィールドをスキップする
- DeepCopy
- 送信元フィールドがnullの場合の挙動
- リスト(Collection)のマージ or 置き換え
- Mapからクラスへのマッピング
- 変換(Converter)の使用
- 条件付き変換(Conditional Mapping)
- プロバイダー(Providers)


### 命名規則に合わないフィールドの組み合わせを指定する

```java
var mapper = new ModelMapper();
mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);

// 組み合わせを追加
mapper.typeMap(Source.class, Target.class)
        .addMapping(Source::getCompanyAddress, Target::setFullName);

// 結果
// PropertyMapping[Source.firstName -> Target.firstName]
// PropertyMapping[Source.companyAddress -> Target.fullName] // 命名規則にないマッピング
```

### 一部のフィールドをスキップする

companyフィールドをマッピング対象から除外する。

```java
var mapper = new ModelMapper();
mapper.createTypeMap(Source.class, Target.class)
        .addMappings(x -> x.skip(Target::setCompany)
        .getMappings().forEach(System.out::println);

// 結果
// PropertyMapping[Source.companyAddress -> Target.address]
// ConstantMapping[null -> Target.company]
// PropertyMapping[Source.firstName -> Target.firstName]
```

### DeepCopy

クラス型のフィールドをコピーする際に新しいオブジェクトを作成する。

```java
var mapper = new ModelMapper();
mapper.getConfiguration().setDeepCopyEnabled(true); // DeepCopyを有効化
```

比較検証
```java
source.setPerson(new Person("abc", "efg"));
var target = mapper.map(source, Target3.class);
source.getPerson().setFirstName("firstName_changed");

// 処理結果(Deep Copy を使わない場合)
// Source3(person=Person(firstName=firstName_changed, lastName=efg))
// Target3(person=Person(firstName=firstName_changed, lastName=efg))

// 処理結果(Deep Copy を使った場合)
// Source3(person=Person(firstName=firstName_changed, lastName=efg))
// Target3(person=Person(firstName=abc, lastName=efg))
```

### 送信元フィールドがnullの場合の挙動

標準設定では、送信元フィールドがnullの場合、nullがセットされる。
```java
var mapper = new ModelMapper();

var source = new Source();
source.setFirstName(null);
var target = new Target();
target.setFirstName("target_firstName");
mapper.map(source, target);
System.out.println("source: " + source.getFirstName());
System.out.println("target: " + target.getFirstName());

// 結果(標準)
// source: null
// target: null
```

nullの場合、マッピングをスキップする設定
```java
var mapper = new ModelMapper();
mapper.getConfiguration().setSkipNullEnabled(true); // nullはスキップ

// 結果
// source: null
// target: target_firstName
```
### リスト(Collection)のマージ or 置き換え

標準設定では、List型のフィールドのコピーは、リストの数が増えた分だけ追加する。

```java
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
// target: [a, b, c, h]
```

```java
var mapper = new ModelMapper();
mapper.getConfiguration().setCollectionsMergeEnabled(false); // Listは常に置き換える

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
```
### コンバーター(Converter)の使用

データ型が異なるフィールド同士のマッピングルールを定義できる。例: 日付型 -> 文字列

https://modelmapper.org/user-manual/converters/

```java
/**
 * List<String> -> String リスト -> カンマ区切り
 */
Converter<List<String>, String> listToString = new AbstractConverter<>() {
  @Override
  protected String convert(List<String> source) {
    return String.join(",", source);
  }
};

var mapper = new ModelMapper();
mapper.addConverter(listToString);
var source = new Source4();
source.setStringList(List.of("a", "b", "c"));
var target = mapper.map(source, Target4_1.class);
System.out.println("source: " + source.getStringList());
System.out.println("target: " + target.getStringList());

// 結果
// source: [a, b, c]
// target: a,b,c      // コンバーターの結果で置換されている
```

### Mapからクラスへのマッピング
```java
var mapper = new ModelMapper();
var source = new LinkedHashMap<>();
source.put("firstName", "firstName_xxx");
var target = mapper.map(source, Target.class);
System.out.println("source: " + source);
System.out.println("target: " + target);
// 結果
// source: source: {firstName=firstName_xxx}
// target: target: Target(firstName=firstName_xxx, fullName=null, address=null, company=null)
```

### 条件付き変換(Conditional Mapping)
https://modelmapper.org/user-manual/property-mapping/#conditional-mapping

### プロバイダー(Providers)
https://modelmapper.org/user-manual/property-mapping/#providers

### Recordへの対応

モジュール追加で対応可能
https://github.com/modelmapper/modelmapper-module-record


## Springで使う方法(JavaConfigによるBean登録)

### maven
```xml
<!--	ModelMapper本体	-->
<dependency>
	<groupId>org.modelmapper</groupId>
	<artifactId>modelmapper</artifactId>
	<version>3.2.0</version>
</dependency>
<!--	Spring統合	-->
<dependency>
	<groupId>org.modelmapper.extensions</groupId>
	<artifactId>modelmapper-spring</artifactId>
	<version>3.2.0</version>
</dependency>
<!--	OptionalなどJava8で追加された機能対応	-->
<dependency>
	<groupId>org.modelmapper</groupId>
	<artifactId>modelmapper-module-java8-datatypes</artifactId>
	<version>1.2.3</version>
</dependency>
<!--	JSR310(LocalDateTime)対応	-->
<dependency>
	<groupId>org.modelmapper</groupId>
	<artifactId>modelmapper-module-jsr310</artifactId>
	<version>1.2.3</version>
</dependency>
<!--	Record対応	-->
<dependency>
	<groupId>org.modelmapper</groupId>
	<artifactId>modelmapper-module-record</artifactId>
	<version>1.0.0</version>
</dependency>
```

### JavaConfig

※以下の実装例は十分な動作確認を行っていません。

```java
package com.example.config;

import org.modelmapper.AbstractConverter;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.modelmapper.convention.NameTransformers;
import org.modelmapper.convention.NamingConventions;
import org.modelmapper.module.jdk8.Jdk8Module;
import org.modelmapper.module.jsr310.Jsr310Module;
import org.modelmapper.module.jsr310.Jsr310ModuleConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Configuration
public class ModelMapperConfig {

    private static final String DATE_TIME_FORMAT = "yyyy/MM/dd HH:mm:ss";
    private static final String DATE_FORMAT = "yyyy/MM/dd";

    /**
     * 標準設定
     *
     * @return ModelMapper
     */
    @Bean("modelMapper")
    ModelMapper modelMapper() {
        return createDefaultSetting();
    }

    /**
     * skipNull有効設定
     *
     * @return ModelMapper
     */
    @Bean("skipNullModelMapper")
    ModelMapper skipNullModelMapper() {
        var mapper = createDefaultSetting();
        mapper.getConfiguration()
                .setSkipNullEnabled(true);
        return mapper;
    }


    /**
     * Builderパターン対応
     *
     * @return ModelMapper
     * @See https://hepokon365.hatenablog.com/entry/2019/02/28/205009
     */
    @Bean("builderModelMapper")
    ModelMapper builderModelMapper() {
        var mapper = createDefaultSetting();
        mapper.getConfiguration()
                .setDestinationNameTransformer(NameTransformers.builder())
                .setDestinationNamingConvention(NamingConventions.builder());

        return mapper;
    }

    /**
     * 標準設定
     *
     * @return ModelMapper
     */
    private ModelMapper createDefaultSetting() {

        var modelMapper = new ModelMapper();

        var config = Jsr310ModuleConfig.builder()
                .dateTimePattern(DATE_TIME_FORMAT) // default is yyyy-MM-dd HH:mm:ss
                .datePattern(DATE_FORMAT) // default is yyyy-MM-dd
                .build();

        modelMapper
                .registerModule(new Jdk8Module())
                .registerModule(new Jsr310Module(config))
                .getConfiguration()
                .setMatchingStrategy(MatchingStrategies.STRICT) // 厳格なマッチングルール
                .setCollectionsMergeEnabled(false) //List型のフィールドを常に上書き
                .setDeepCopyEnabled(true) // DeepCopyを有効
        ;

        // カスタムコンバーターの追加
        modelMapper.addConverter(setToString);
        modelMapper.addConverter(stringToSet);
        modelMapper.addConverter(listToString);
        modelMapper.addConverter(stringToList);

        // カスタムマッピングの追加
        // modelMapper.typeMap(Source.class, Target.class)
        //        .addMapping(Source::getFiedlA, Target::setFieldB);

        return modelMapper;
    }


    /**
     * Set<String> -> String Set -> カンマ区切り
     */
    Converter<Set<String>, String> setToString = new AbstractConverter<>() {
        @Override
        protected String convert(Set<String> source) {
            return String.join(",", source);
        }
    };

    /**
     * String -> Set<String> カンマ区切り -> Set
     */
    Converter<String, Set<String>> stringToSet = new AbstractConverter<>() {
        @Override
        protected Set<String> convert(String source) {
            return Set.copyOf(Arrays.asList(source.split(",", 0)));
        }
    };

    /**
     * List<String> -> String リスト -> カンマ区切り
     */
    Converter<List<String>, String> listToString = new AbstractConverter<>() {
        @Override
        protected String convert(List<String> source) {
            return String.join(",", source);
        }
    };

    /**
     * String -> List<String> カンマ区切り -> リスト
     */
    Converter<String, List<String>> stringToList = new AbstractConverter<>() {
        @Override
        protected List<String> convert(String source) {
            return Arrays.asList(source.split(",", 0));
        }
    };

    /**
     * 大文字に変換(String -> String)
     *
     * @see <a href="https://modelmapper.org/user-manual/property-mapping/#converters">Referrence</a>
     */
    Converter<String, String> toUppercase =
            ctx -> ctx.getSource() == null ? null : ctx.getSource().toUpperCase();

}
```

### デバッグログの出力

ModelMapper実行後にフィールドのマッチング結果をログに出力する方法。
```java
mapper.getTypeMap(Source.class, Target.class).getMappings().forEach(x -> log.debug(x.toString()));
```

## 最後に

十分にテストして使ってください。


## 参考資料
- https://modelmapper.org/
- https://github.com/modelmapper/modelmapper
- https://hepokon365.hatenablog.com/entry/2019/02/28/205009
- https://isaacbroyles.com/2018/07/15/model-mapper.html
- https://stackoverflow.com/questions/44534172/how-to-customize-modelmapper/44534173#44534173
- https://web.archive.org/web/20200128093810/http://www.talangsoft.org/2015/04/20/domain_mapping_with_modelmapper/
- https://qiita.com/euledge/items/482a113589015590cf19
