typedef heapMeta as struct {
	int size;
	bool free;
	bool mark;
	heapMeta *next;
}

/* There are local vars */
void *lowerStackAddress;
/* There are global vars */
void *dataEndAddress;
/* Heap memory region */
void *heapMemoryStart;
void *heapMemoryEnd;
/* Heap memory blocks (linked list) */
heapMeta *heapMemory;

function bool IsStackAddress(void *value, void *higherStackAddress)
{
	if(value <= higherStackAddress && value > lowerStackAddress){
		return true;
	}
	
	return false;
}

function bool IsHeapAddress(void *value)
{
	if(value >= heapMemoryStart){
		if(value < heapMemoryEnd){
			return true;
		}
	}
	
	return false;
}

function bool isBlockFree() (heapMeta)
{
	if(this.size & 0x80000000 > 0){
		return true;
	}
	return false;
}

function void setBlockFree() (heapMeta)
{
	this.size = this.size | 0x80000000;
}

function void unsetBlockFree() (heapMeta)
{
	this.size = this.size & 0x7FFFFFFF;
}

function bool isBlockMarked() (heapMeta)
{
	if(this.size & 0x40000000 > 0){
		return true;
	}
	return false;
}

function void setBlockMarked() (heapMeta)
{
	this.size = this.size | 0x40000000;
}

function void unmarkBlock() (heapMeta)
{
	this.size = this.size & 0xBFFFFFFF;
}

function void setBlockFreeUnmarked() (heapMeta)
{
	this.size = this.size & 0x3FFFFFFF;
}

function int getSize(int size) (heapMeta)
{
	return this.size & 0x3FFFFFFF;
}

function void GCInit(void *stackStart)
{
	lowerStackAddress = stackStart;
	
	__asm__("mov DWORD [gc_heapMemoryStart], __data_start");
	__asm__("mov DWORD [gc_heapMemoryEnd], _end");
	printf("heap start address: 0x%x\n", heapMemoryStart);
	printf("heap end address: 0x%x\n", heapMemoryEnd);
}

function heapMeta* findFreeBlock()
{
	return null;
}
