package com.gxa.pipe.dataVsualization.dashboard;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AreaScopeResolverTest {

    private final AreaScopeResolver resolver = new AreaScopeResolver();

    @Test
    void nullAreaResolvesToCountry() {
        AreaScope scope = resolver.resolve(null);

        assertThat(scope.level()).isEqualTo(AreaScope.Level.COUNTRY);
        assertThat(scope.rollupAreaId()).isEqualTo(100000L);
        assertThat(scope.areaStart()).isEqualTo(100000L);
        assertThat(scope.areaEnd()).isEqualTo(999999L);
    }

    @Test
    void provinceAreaResolvesRange() {
        AreaScope scope = resolver.resolve(370000L);

        assertThat(scope.level()).isEqualTo(AreaScope.Level.PROVINCE);
        assertThat(scope.rollupAreaId()).isEqualTo(370000L);
        assertThat(scope.areaStart()).isEqualTo(370000L);
        assertThat(scope.areaEnd()).isEqualTo(379999L);
    }

    @Test
    void cityAreaResolvesRange() {
        AreaScope scope = resolver.resolve(370100L);

        assertThat(scope.level()).isEqualTo(AreaScope.Level.CITY);
        assertThat(scope.rollupAreaId()).isEqualTo(370100L);
        assertThat(scope.areaStart()).isEqualTo(370100L);
        assertThat(scope.areaEnd()).isEqualTo(370199L);
    }

    @Test
    void districtAreaResolvesExactRange() {
        AreaScope scope = resolver.resolve(370102L);

        assertThat(scope.level()).isEqualTo(AreaScope.Level.DISTRICT);
        assertThat(scope.rollupAreaId()).isEqualTo(370102L);
        assertThat(scope.areaStart()).isEqualTo(370102L);
        assertThat(scope.areaEnd()).isEqualTo(370102L);
    }
}
