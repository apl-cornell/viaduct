VIADUCT := $(if $(wildcard ../viaduct), ../viaduct, viaduct)

BENCHMARK_DIR := benchmarks
BENCHMARKS := $(sort $(wildcard $(BENCHMARK_DIR)/*.via))
ERASED_BENCHMARKS := $(sort $(wildcard $(BENCHMARK_DIR)/*Erased.via))
ANNOTATED_BENCHMARKS := $(filter-out $(ERASED_BENCHMARKS), $(BENCHMARKS))

BUILD_DIR := build
LAN_BUILD_DIR := $(BUILD_DIR)/lan
WAN_BUILD_DIR := $(BUILD_DIR)/wan

.PHONY: all
all: lan wan erased

.PHONY: lan wan erased
lan: $(subst $(BENCHMARK_DIR), $(LAN_BUILD_DIR), $(ANNOTATED_BENCHMARKS))
wan: $(subst $(BENCHMARK_DIR), $(WAN_BUILD_DIR), $(ANNOTATED_BENCHMARKS))
erased: $(subst $(BENCHMARK_DIR), $(LAN_BUILD_DIR), $(ERASED_BENCHMARKS))

$(LAN_BUILD_DIR)/%.via: $(BENCHMARK_DIR)/%.via $(LAN_BUILD_DIR)
	$(VIADUCT) -v compile $< -o $@

$(WAN_BUILD_DIR)/%.via: $(BENCHMARK_DIR)/%.via $(WAN_BUILD_DIR)
	$(VIADUCT) -v compile $< -o $@ --wancost

$(LAN_BUILD_DIR) $(WAN_BUILD_DIR):
	mkdir -p $@

.PHONY: clean
clean:
	rm -rf $(LAN_BUILD_DIR)
	rm -rf $(WAN_BUILD_DIR)
