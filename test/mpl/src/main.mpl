import "mem/gc"

function int gimmeInt(int x, int y)
{
	return x + y;
}

typedef Bakas as struct {
	int talpa;
}

typedef Masina as struct {
	int ratuDydis;
	int greitis;
	Bakas kuroBakas;
}

function setInfo(int ratuDydis, int greitis, int bakoTalpa) (Masina)
{
	this.ratuDydis = ratuDydis;
	this.greitis = greitis;
	this.kuroBakas.talpa = bakoTalpa;
}

function printInfo() (Masina)
{
	printf("rDydis: %d, greitis: %d, bako talpa: %d\n", this.ratuDydis, this.greitis, this.kuroBakas.talpa);
}

int heapVariable = 5;

function main()
{
	gc.GCInit(0);

	int a = 10;
	int b = 5;
	int c = ((gimmeInt(a / b, a / b - 1) * 123) / 13 + b / 2 * 7) * ((gimmeInt(a / b, 1) * 123) / 13 + b / 2 * 7);
	
	printf("c: %d, b: %d\n", c, b);
	
	// Boolean algebra
	int boolTest = -123;
	
	bool test = boolTest;
	if(test == true){
		printf("test\n");
	}else{
		printf("no test!\n");
	}
	
	bool *bp = &test;
	if(*bp == true){
		printf("ayay\n");
	}
	
	
	
	Masina m;
	
	m.ratuDydis = 18;
	m.greitis = 200;
	m.kuroBakas.talpa = 68;
	printf("&m: %x\n", &m);
	printf("&m.greitis: %x\n", &m.greitis);
	m.printInfo();
	
	m.setInfo(15, 113, 70);
	m.printInfo();
	
	printf("\n");
	
	Masina *mPtr = &m;
	//int sudas = (int)(*(mPtr + 4));
	
	printf("*mPtr: %d, *(mPtr+8): %d\n", *mPtr, *(mPtr+8));
	
	Masina ms[2];
	ms[0].setInfo(11, 111, 1111);
	ms[1].setInfo(23, 232, 3232);
	
	int i=0;
	while(i < (sizeof(ms) / sizeof(m))){
		printf("&ms[%d]: %x\n", i, &ms[i]);
		i++;
	}
	
	for(int z=0; z<(sizeof(ms) / sizeof(m)); z++){
		ms[z].printInfo();
	}
	
	for bibis in ms {
		bibis.printInfo();
	}
}

function isHeapAddress(void *address)
{
	bool heapAddress = gc.IsHeapAddress(address);
	if(heapAddress == true){
		_c_printf("It's a heap address!\n");
	}else{
		_c_printf("It's not a heap address!\n");
	}
}
