import "mem/gc"

function int gimmeInt(int x, int y)
{
	return x + y;
}

typedef FuelTank as struct {
	int capacity;
}

typedef Car as struct {
	int wheelSize;
	int speed;
	FuelTank fuelTank;
}

function setInfo(int wheelSize, int speed, int fuelTankCapacity) (Car)
{
	this.wheelSize = wheelSize;
	this.speed = speed;
	this.fuelTank.capacity = fuelTankCapacity;
}

function printInfo() (Car)
{
	printf("wheel size: %d, speed: %d, fuel tank capacity: %d\n", this.wheelSize, this.speed, this.fuelTank.capacity);
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
	
	
	
	Car car;
	
	car.wheelSize = 18;
	car.speed = 200;
	car.fuelTank.capacity = 68;
	printf("&car: %x\n", &car);
	printf("&car.greitis: %x\n", &car.speed);
	car.printInfo();
	
	car.setInfo(15, 113, 70);
	car.printInfo();
	
	printf("\n");
	
	Car *cPtr = &car;
	//int sudas = (int)(*(mPtr + 4));
	
	printf("*cPtr: %d, *(cPtr+8): %d\n", *cPtr, *(cPtr+8));
	
	Car cs[2];
	cs[0].setInfo(11, 111, 1111);
	cs[1].setInfo(23, 232, 3232);
	
	int i=0;
	while(i < (sizeof(cs) / sizeof(car))){
		printf("&cs[%d]: %x\n", i, &cs[i]);
		i++;
	}
	
	for(int z=0; z<(sizeof(cs) / sizeof(car)); z++){
		cs[z].printInfo();
	}
	
	for carr in cs {
		carr.printInfo();
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
