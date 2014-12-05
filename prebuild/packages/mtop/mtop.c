#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <errno.h>
#include <unistd.h>

#include <cutils/properties.h>

#define MTOP_VERSION "0.5"

typedef struct NodeInfo {
	int	 id;
	char *name;
	FILE *fp;
	unsigned int curcnt;
	unsigned int precnt;
	unsigned long long delta;
} NodeInfo;

NodeInfo nodeInfo_sun9i[] = {
	{ 0, "cpuddr0",  NULL, 0, 0, 0},
	{ 1, "gpuddr0",  NULL, 0, 0, 0},
	{ 2, "de_ddr0",  NULL, 0, 0, 0},
	{ 3, "vcfddr0",  NULL, 0, 0, 0},
	{ 4, "othddr0",  NULL, 0, 0, 0},
	{ 5, "cpuddr1",  NULL, 0, 0, 0},
	{ 6, "gpuddr1",  NULL, 0, 0, 0},
	{ 7, "de_ddr1",  NULL, 0, 0, 0},
	{ 8, "vcfddr1",  NULL, 0, 0, 0},
	{ 9, "othddr1",  NULL, 0, 0, 0},
};

NodeInfo nodeInfo_sun8i[] = {
	{ 0, "totddr", NULL, 0, 0, 0},
	{ 1, "cpuddr", NULL, 0, 0, 0},
	{ 2, "gpuddr", NULL, 0, 0, 0},
	{ 3, "de_ddr", NULL, 0, 0, 0},
	{ 4, "ve_ddr", NULL, 0, 0, 0},
	{ 5, "othddr", NULL, 0, 0, 0},
};

NodeInfo *nodeInfo;

unsigned int max;
unsigned long long total;
unsigned long long idx;

int nodeCnt;

int delay;
int iter;
int mUnit;
int latency;
unsigned int per;
char output_fn[256];
FILE *output_fp;

int nhardware;
char path_prefix[256];

#define GTBUS_PMU_DIR "/sys/devices/platform/GTBUS_PMU"
#define MBUS_PMU_DIR "/sys/devices/platform/MBUS_PMU"

static int mtop_baner();
static int mtop_read();
static void mtop_post();
static void mtop_update();

static void usage(char *program)
{
	fprintf(stdout, "\n");
	fprintf(stdout, "Usage: %s [-n iter] [-d delay] [-m] [-o FILE] [-h]\n", program);
	fprintf(stdout, "    -n NUM   Updates to show before exiting.\n");
	fprintf(stdout, "    -d NUM   Seconds to wait between update.\n");
	fprintf(stdout, "    -m unit: MB\n");
	fprintf(stdout, "    -o FILE  Output to a file.\n");
	fprintf(stdout, "    -v Display mtop version.\n");
	fprintf(stdout, "    -h Display this help screen.\n");
	fprintf(stdout, "\n");
}

static void version(void)
{
	fprintf(stdout, "\n");
	fprintf(stdout, "mtop version: %s\n", MTOP_VERSION);
	fprintf(stdout, "\n");
}

int main(int argc, char *argv[])
{
	int i;
	unsigned long value, bandwidth;
	char hardware[PROPERTY_VALUE_MAX];

	property_get("ro.hardware", hardware, "sun9i");
	if (!strcmp(hardware, "sun9i")) {
		nhardware = 0;
		nodeCnt = sizeof(nodeInfo_sun9i)/sizeof(nodeInfo_sun9i[0]);
		strncpy(path_prefix, GTBUS_PMU_DIR, sizeof(path_prefix));
		nodeInfo = nodeInfo_sun9i;
	} else if (!strcmp(hardware, "sun8i")) {
		nhardware = 1;
		nodeCnt = sizeof(nodeInfo_sun8i)/sizeof(nodeInfo_sun8i[0]);
		strncpy(path_prefix, MBUS_PMU_DIR, sizeof(path_prefix));
		nodeInfo = nodeInfo_sun8i;
	} else {
		nhardware = 0;
		nodeCnt = sizeof(nodeInfo_sun9i)/sizeof(nodeInfo_sun9i[0]);
		strncpy(path_prefix, GTBUS_PMU_DIR, sizeof(path_prefix));
		nodeInfo = nodeInfo_sun9i;
	}

	max = 0;
	total = 0;
	idx = 0;

	delay = 1;
	iter = -1;
	mUnit = 0;
	latency = 0;
	per = 0xffffffff;
	memset(output_fn, 0, sizeof(output_fn));
	output_fp = NULL;

	for (i = 1; i < argc; i++) {
		if (!strcmp(argv[i], "-n")) {
			if (i + 1 >= argc) {
				fprintf(stderr, "Option -n expects an argument.\n");
				usage(argv[0]);
				exit(-1);
			}
			iter = atoi(argv[++i]);
			// FIXME
			continue;
		}

		if (!strcmp(argv[i], "-d")) {
			if (i + 1 >= argc) {
				fprintf(stderr, "Option -d expects an argument.\n");
				usage(argv[0]);
				exit(-1);
			}
			delay = atoi(argv[++i]);
			// FIXME
			continue;
		}

		if (!strcmp(argv[i], "-m")) {
			mUnit = 1;
			continue;
		}

		if (!strcmp(argv[i], "-l")) {
			latency = 1;
			continue;
		}
/*
		if (!strcmp(argv[i], "-p")) {
			per = 1;
			continue;
		}
*/
		if (!strcmp(argv[i], "-o")) {
			if (i + 1 >= argc) {
				fprintf(stderr, "Option -o expects an argument.\n");
				usage(argv[0]);
				exit(-1);
			}
			strncpy(output_fn, argv[++i], 256);
			continue;
		}

		if (!strcmp(argv[i], "-v")) {
			version();
			exit(0);
		}

		if (!strcmp(argv[i], "-h")) {
			usage(argv[0]);
			exit(0);
		}

		fprintf(stderr, "Invalid argument \"%s\".\n", argv[i]);
		usage(argv[0]);
		exit(-1);
	}

	fprintf(stdout, "\n");
	fprintf(stdout, "iter: %d\n", iter);
	fprintf(stdout, "dealy: %d\n", delay);
	fprintf(stdout, "unit: %s\n", mUnit ? "MB" : "KB");
	fprintf(stdout, "output: %s\n", output_fn);
	fprintf(stdout, "\n");

	if (output_fn[0]) {
		output_fp = fopen(output_fn, "w");
		if (NULL == output_fp) {
			fprintf(stdout, "Could not open file %s: %s\n", output_fn, strerror(errno));
			exit(-1);
		}

		mtop_baner();
	}

	mtop_read();
	mtop_post();

	while (iter == -1 || iter-- > 0) {
		sleep(delay);
		mtop_read();
		mtop_update();
		mtop_post();
	}

	if (output_fp) {
		fclose(output_fp);
	}

	return 0;
}

static int mtop_baner(void)
{
	int i;

	for (i = 0; i < nodeCnt; i++) {
		if (per & (1UL << i)) {
			fwrite(nodeInfo[i].name, strlen(nodeInfo[i].name), 1, output_fp);
			if (i+1 < nodeCnt && per & (1UL << (i+1))) {
				fwrite(" ", 1, 1, output_fp);
			}
		}
	}

	fwrite("\n", 1, 1, output_fp);

	return 0;
}

static int mtop_read(void)
{
	int i;
	char path[256];

	for (i = 0; i < nodeCnt; i++) {
		if (per & (1UL << i)) {
			snprintf(path, sizeof(path), "%s/pmu_%s", path_prefix, nodeInfo[i].name);
			nodeInfo[i].fp = fopen(path, "r");
			if (NULL == nodeInfo[i].fp) {
				fprintf(stderr, "Could not open file %s: %s\n", path, strerror(errno));
				goto open_error;
			}

			fscanf(nodeInfo[i].fp, "%u", &nodeInfo[i].curcnt);
			fclose(nodeInfo[i].fp);
			nodeInfo[i].fp = NULL;
		}
	}

	return 0;

open_error:
	for (i = 0; i < nodeCnt; i++) {
		if (nodeInfo[i].fp) {
			fclose(nodeInfo[i].fp);
			nodeInfo[i].fp = NULL;
		}
	}
	return -1;
}

static void mtop_post(void)
{
	int i;

	for (i = 0; i < nodeCnt; i++) {
		if (per & (1UL << i)) {
			nodeInfo[i].precnt = nodeInfo[i].curcnt;
		}
	}
}

static void mtop_update(void)
{
	int i;
	unsigned int cur_total;
	unsigned int average;
	char buf[1024];

	cur_total = 0;

	for (i = 0; i < nodeCnt; i++) {
		if (per & (1UL << i)) {
			if (nodeInfo[i].precnt < nodeInfo[i].curcnt)
				nodeInfo[i].delta = nodeInfo[i].curcnt - nodeInfo[i].precnt;
			else
				nodeInfo[i].delta = (nodeInfo[i].curcnt + (unsigned long long)(2^32)) - nodeInfo[i].precnt;

			if (mUnit)
				nodeInfo[i].delta >>= 10;

			if (nhardware == 0) {
				cur_total += nodeInfo[i].delta;
			}
		}
	}

	if (nhardware == 0) {
		if (cur_total > max)
			max = cur_total;
		total += cur_total;
		idx++;
		average = total / idx;
		
		fprintf(stdout, "Max: %u, Average: %u\n", max, average);
		fprintf(stdout, "Current Total: %u\n", cur_total);
		fprintf(stdout, "%s %s %s %s %s %s %s %s %s %s\n", nodeInfo[0].name,
				nodeInfo[1].name, nodeInfo[2].name, nodeInfo[3].name,
				nodeInfo[4].name, nodeInfo[5].name, nodeInfo[6].name,
				nodeInfo[7].name, nodeInfo[8].name, nodeInfo[9].name);
		snprintf(buf, sizeof(buf), "%7llu %7llu %7llu %7llu %7llu %7llu %7llu %7llu %7llu %7llu",
				nodeInfo[0].delta, nodeInfo[1].delta, nodeInfo[2].delta,
				nodeInfo[3].delta, nodeInfo[4].delta, nodeInfo[5].delta,
				nodeInfo[6].delta, nodeInfo[7].delta, nodeInfo[8].delta,
				nodeInfo[9].delta);
		fprintf(stdout, "%s\n", buf);
		if (output_fp) {
			fwrite(buf, strlen(buf), 1, output_fp);
		}

		if (cur_total == 0)
			cur_total++;
		fprintf(stdout, "%7.2f %7.2f %7.2f %7.2f %7.2f %7.2f %7.2f %7.2f %7.2f %7.2f\n",
				(float)nodeInfo[0].delta*100/cur_total, (float)nodeInfo[1].delta*100/cur_total,
				(float)nodeInfo[2].delta*100/cur_total, (float)nodeInfo[3].delta*100/cur_total,
				(float)nodeInfo[4].delta*100/cur_total, (float)nodeInfo[5].delta*100/cur_total,
				(float)nodeInfo[6].delta*100/cur_total, (float)nodeInfo[7].delta*100/cur_total,
				(float)nodeInfo[8].delta*100/cur_total, (float)nodeInfo[9].delta*100/cur_total);
		fprintf(stdout, "\n");
	} else if (nhardware == 1) {
		fprintf(stdout, "%7s %7s %7s %7s %7s %7s\n", nodeInfo[0].name,
				nodeInfo[1].name, nodeInfo[2].name, nodeInfo[3].name,
				nodeInfo[4].name, nodeInfo[5].name);
		snprintf(buf, sizeof(buf), "%7llu %7llu %7llu %7llu %7llu %7llu",
				nodeInfo[0].delta, nodeInfo[1].delta, nodeInfo[2].delta,
				nodeInfo[3].delta, nodeInfo[4].delta, nodeInfo[5].delta);
		fprintf(stdout, "%s\n", buf);

		if (output_fp) {
			fwrite(buf, strlen(buf), 1, output_fp);
		}

		fprintf(stdout, "\n");
	} else {
		// TODO
	}

	if (output_fp) {
		fwrite("\n", 1, 1, output_fp);
		fflush(output_fp);
	}
}
