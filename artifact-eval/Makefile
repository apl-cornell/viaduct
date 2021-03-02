VIADUCT := $(if $(wildcard ../viaduct), ../viaduct, viaduct)

BENCHMARK_DIR := benchmarks
BENCHMARKS := $(wildcard $(BENCHMARK_DIR)/*.via)

BUILD_DIR := build
LAN_BUILD_DIR := $(BUILD_DIR)/lan
WAN_BUILD_DIR := $(BUILD_DIR)/wan

.PHONY: lan
lan: $(subst $(BENCHMARK_DIR), $(LAN_BUILD_DIR), $(BENCHMARKS))

.PHONY: wan
wan: $(subst $(BENCHMARK_DIR), $(WAN_BUILD_DIR), $(BENCHMARKS))

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
