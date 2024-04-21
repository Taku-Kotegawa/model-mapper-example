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
import org.modelmapper.record.RecordModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Configuration
public class ModelMapperConfig {

    public static final String MODEL_MAPPER = "modelMapper";
    public static final String SKIP_NULL_MODEL_MAPPER = "skipNullModelMapper";
    public static final String BUILDER_MODEL_MAPPER = "builderModelMapper";

    private static final String DATE_TIME_FORMAT = "yyyy/MM/dd HH:mm:ss";
    private static final String DATE_FORMAT = "yyyy/MM/dd";


    @Bean
    ModelMapperHolder modelMapperHolder() {
        return new ModelMapperHolder(
                Map.of(
                        MODEL_MAPPER, modelMapper(),
                        SKIP_NULL_MODEL_MAPPER, skipNullModelMapper(),
                        BUILDER_MODEL_MAPPER, builderModelMapper()
                )
        );
    }


    /**
     * 標準設定
     *
     * @return ModelMapper
     */
    @Bean(MODEL_MAPPER)
    ModelMapper modelMapper() {
        return createDefaultSetting();
    }

    /**
     * skipNull有効設定
     *
     * @return ModelMapper
     */
    @Bean(SKIP_NULL_MODEL_MAPPER)
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
    @Bean(BUILDER_MODEL_MAPPER)
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
                .registerModule(new RecordModule())
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
